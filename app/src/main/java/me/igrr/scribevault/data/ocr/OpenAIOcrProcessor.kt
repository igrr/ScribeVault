package me.igrr.scribevault.data.ocr

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import me.igrr.scribevault.domain.ocr.OcrProcessor
import me.igrr.scribevault.domain.ocr.OcrResult
import me.igrr.scribevault.data.preferences.UserPreferencesRepository
import com.aallam.openai.api.chat.ContentPart
import com.aallam.openai.api.chat.ImagePart
import com.aallam.openai.api.chat.TextPart


class OpenAIOcrProcessor(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) : OcrProcessor {

    private val prompt = """
        You are an expert handwriting transcriber model.
        The input from the user will be a photo of a handwritten page — such as a personal journal, notes from a meeting, or thoughts on some topic.
        Your task is to respond with the textual representation of that handwriting.
        Use markdown syntax. Don't wrap the whole output into code blocks.
        If there are diagrams on the page, describe them using a Mermaid diagram code block or a code block with ASCII art.

        IMPORTANT: Return ONLY the transcribed content for this single page. Do NOT include any metadata (no tags, no title).
    """

    override suspend fun processImage(imageUri: Uri): OcrResult? {
        // Retrieve API token from preferences
        val token = preferencesRepository.getApiKey() ?: return null

        // Create an instance of OpenAI client using the token
        val openAI = OpenAI(token)

        // Read the image data from the provided Uri
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
        val imageBytes = inputStream.readBytes()
        inputStream.close()

        // Encode image bytes to Base64 string
        val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

        // Determine MIME type, defaulting to image/jpeg if unavailable
        val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"

        // Prepare chat messages with a system prompt and a user prompt that includes the image
        val systemMessage = ChatMessage(
            role = ChatRole.System,
            content = prompt
        )

        val reqList: ArrayList<ContentPart> = ArrayList<ContentPart>()
        reqList.add(TextPart("Please transcribe the following handwritten note"))
        reqList.add(ImagePart("data:$mimeType;base64,$base64Image"))

        val userMessage = ChatMessage(
            role = ChatRole.User,
            content = reqList
        )

        val request = ChatCompletionRequest(
            model = ModelId("gpt-4o-mini"),
            messages = listOf(systemMessage, userMessage)
        )

        Log.d("OpenAIOcrProcessor", "Request: ${request.messages}")

        // Call the OpenAI Chat Completion API
        val completion: ChatCompletion = openAI.chatCompletion(request)

        // Log the details of the response for debugging
        Log.d("OpenAIOcrProcessor", "Number of choices: ${completion.choices.size}")
        Log.d("OpenAIOcrProcessor", "First choice: ${completion.choices.firstOrNull()?.message?.content}")
        Log.d("OpenAIOcrProcessor", "First choice role: ${completion.choices.firstOrNull()?.message?.role}")
        Log.d("OpenAIOcrProcessor", "First choice finish reason: ${completion.choices.firstOrNull()?.finishReason}")
        Log.d("OpenAIOcrProcessor", "First choice index: ${completion.choices.firstOrNull()?.index}")
        Log.d("OpenAIOcrProcessor", "First choice logprobs: ${completion.choices.firstOrNull()?.logprobs}")

        val text = completion.choices.firstOrNull()?.message?.content ?: ""
        // Return only the page content. Title/tags are generated later for the combined text.
        return OcrResult(
            content = text,
            tags = emptyList(),
            title = ""
        )


    }

    suspend fun startSession(): ChatTranscriptionSession? {
        val token = preferencesRepository.getApiKey() ?: return null
        val openAI = OpenAI(token)
        return ChatTranscriptionSession(openAI)
    }

    inner class ChatTranscriptionSession(private val openAI: OpenAI) {
        private val messages = mutableListOf<ChatMessage>()

        init {
            messages += ChatMessage(role = ChatRole.System, content = prompt)
        }

        suspend fun transcribePage(imageUri: Uri, pageIndex: Int): String? {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
            val imageBytes = inputStream.readBytes()
            inputStream.close()
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"

            val contentParts = arrayListOf<ContentPart>()
            contentParts.add(TextPart("Transcribe only the content for page $pageIndex. Do not include any metadata."))
            contentParts.add(ImagePart("data:$mimeType;base64,$base64Image"))
            val userMessage = ChatMessage(role = ChatRole.User, content = contentParts)
            messages += userMessage

            val request = ChatCompletionRequest(model = ModelId("gpt-4o-mini"), messages = messages)
            val completion: ChatCompletion = openAI.chatCompletion(request)
            val assistantContent = completion.choices.firstOrNull()?.message?.content ?: return null
            messages += ChatMessage(role = ChatRole.Assistant, content = assistantContent)
            return assistantContent
        }

        suspend fun generateTitleAndTags(): Pair<String, List<String>> {
            val ask = ChatMessage(
                role = ChatRole.User,
                content = listOf<ContentPart>(
                    TextPart(
                        "Based on the pages transcribed above, respond with exactly two lines:\n" +
                            "title: <concise title 1–5 words>\n" +
                            "tags: <comma-separated keywords>"
                    )
                )
            )
            messages += ask
            val request = ChatCompletionRequest(model = ModelId("gpt-4o-mini"), messages = messages)
            val completion = openAI.chatCompletion(request)
            val content = completion.choices.firstOrNull()?.message?.content.orEmpty()
            messages += ChatMessage(role = ChatRole.Assistant, content = content)

            var title = ""
            var tags: List<String> = emptyList()
            try {
                val lines = content.lines()
                val titleLine = lines.firstOrNull { it.startsWith("title:", ignoreCase = true) } ?: ""
                Log.d("OpenAIOcrProcessor", "titleLine: $titleLine")
                val tagsLine = lines.firstOrNull { it.startsWith("tags:", ignoreCase = true) } ?: ""
                Log.d("OpenAIOcrProcessor", "tagsLine: $tagsLine")
                title = titleLine.substringAfter(":").trim()
                tags = tagsLine.substringAfter(":").split(',').map { it.trim() }.filter { it.isNotEmpty() }
            } catch (e: Exception) {
                Log.e("OpenAIOcrProcessor", "Failed to parse session title/tags", e)
            }
            return title to tags
        }
    }
}