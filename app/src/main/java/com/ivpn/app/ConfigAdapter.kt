package com.ivpn.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ivpn.app.util.V2RayConfigUtil.ConfigItem
import com.ivpn.app.R

class ConfigAdapter(
    private var items: List<ConfigItem>,
    private val onSelect: (ConfigItem) -> Unit
) : RecyclerView.Adapter<ConfigAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvConfigName)
        val type: TextView = v.findViewById(R.id.tvConfigType)
        val ping: TextView = v.findViewById(R.id.tvPing)
        val icon: ImageView = v.findViewById(R.id.imgSelect)
        val card: View = v
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_config, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name.ifEmpty { "Server ${position + 1}" }
        
        if (item.ping > 0) {
            holder.ping.text = "${item.ping} ms"
            holder.ping.setTextColor(if (item.ping < 500) Color.GREEN else Color.YELLOW)
        } else {
            holder.ping.text = ""
            holder.ping.setTextColor(Color.GRAY)
        }

        if (item.isSelected) {
            holder.icon.setImageResource(android.R.drawable.radiobutton_on_background)
            holder.icon.setColorFilter(Color.parseColor("#4F46E5"))
        } else {
            holder.icon.setImageResource(android.R.drawable.radiobutton_off_background)
            holder.icon.setColorFilter(Color.GRAY)
        }

        holder.card.setOnClickListener { onSelect(item) }
    }

    override fun getItemCount() = items.size
    
    fun updateList(newItems: List<ConfigItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
