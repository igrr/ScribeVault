package me.igrr.scribevault.ui.capture

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import me.igrr.scribevault.R

class CapturedPagesAdapter(
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<CapturedPagesAdapter.ViewHolder>() {

    private val items: MutableList<Uri> = mutableListOf()

    fun submitList(newItems: List<Uri>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_captured_page, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = items[position]
        holder.image.setImageURI(uri)
        holder.deleteButton.setOnClickListener { onRemove(position) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image_thumbnail)
        val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)
    }
}


