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

        At the end of the output, add two lines of metadata in the following format:
        
        tags: put keywords which describe the transcribed page here, separated by commas
        title: put the recommended title under which the note should be filed; keep it concise, ideally between 1 to 5 words
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

        val content = completion.choices.firstOrNull()?.message?.content ?: ""
        // extract the metadata lines from the content and remove them from the content
        // catch possible errors in the metadata extraction

        var text = ""
        var tags = listOf<String>()
        var title = ""
        try {
            val lines = content.split("\n")
            // find the last line that starts with "tags: "
            val tagsLine = lines.last { it.startsWith("tags: ") }
            tags = tagsLine.split(": ")[1].split(",").map { it.trim() }
            // find the last line that starts with "title: "
            val titleLine = lines.last { it.startsWith("title: ") }
            title = titleLine.split(": ")[1].trim()
            // remove the metadata lines from the lines list
            lines.filter { !it.startsWith("tags: ") && !it.startsWith("title: ") }
            // join the remaining lines into a single string
            text = lines.joinToString("\n")
        } catch (e: Exception) {
            Log.e("OpenAIOcrProcessor", "Error extracting metadata", e)
            return null
        }

        // return the ocr result
        return OcrResult(
            content = text,
            tags = tags,
            title = title
        )


    }
}