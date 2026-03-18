package com.example.wattmeter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChargingSession(
    val date: Long,
    val fromPercent: Int,
    val toPercent: Int,
    val durationMinutes: Int,
    val avgWatts: Double,
    val peakWatts: Double
)

class SessionAdapter(private val sessions: List<ChargingSession>) :
    RecyclerView.Adapter<SessionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvFrom: TextView = view.findViewById(R.id.tvFrom)
        val tvTo: TextView = view.findViewById(R.id.tvTo)
        val tvAvgWatts: TextView = view.findViewById(R.id.tvAvgWatts)
        val tvPeakWatts: TextView = view.findViewById(R.id.tvPeakWatts)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        val sdf = SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.US)
        holder.tvDate.text = sdf.format(Date(session.date))
        val hours = session.durationMinutes / 60
        val minutes = session.durationMinutes % 60
        holder.tvDuration.text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        holder.tvFrom.text = "${session.fromPercent}%"
        holder.tvTo.text = "${session.toPercent}%"
        holder.tvAvgWatts.text = String.format(Locale.US, "%.1fW", session.avgWatts)
        holder.tvPeakWatts.text = String.format(Locale.US, "%.1fW", session.peakWatts)
    }

    override fun getItemCount() = sessions.size
}

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val rootView = findViewById<View>(R.id.recyclerView).parent as View
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top)
            insets
        }

        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }

        val sessions = SessionManager.getSessions(this).reversed()
        val recycler = findViewById<RecyclerView>(R.id.recyclerView)
        val empty = findViewById<TextView>(R.id.tvEmpty)

        if (sessions.isEmpty()) {
            recycler.visibility = View.GONE
            empty.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            empty.visibility = View.GONE
            recycler.layoutManager = LinearLayoutManager(this)
            recycler.adapter = SessionAdapter(sessions)
        }

        findViewById<TextView>(R.id.tvClearAll).setOnClickListener {
            SessionManager.clearSessions(this)
            recycler.visibility = View.GONE
            empty.visibility = View.VISIBLE
        }
    }
}