package design.sandwwraith.networklistdemo

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.TextView
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import design.sandwwraith.networklistdemo.dummy.Photo
import kotlinx.android.synthetic.main.picture_list.*
import kotlinx.android.synthetic.main.picture_list_content.view.*
import java.lang.ref.WeakReference
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [PictureDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class PictureListActivity : AppCompatActivity() {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_list)

        if (picture_detail_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }

        // Напишите unsplash.api.key="ВАШ_АПИ_КЛЮЧ_В_КАВЫЧКАХ" в local.properties
        UnsplashAsyncTask(WeakReference(this)).execute("https://api.unsplash.com/photos?per_page=30&client_id=${BuildConfig.UnsplashApiKey}")
    }

    private fun setupRecyclerView(content: List<Photo>) {
        picture_list.adapter = SimpleItemRecyclerViewAdapter(this, content, twoPane)
    }


    class UnsplashAsyncTask(private val activityRef: WeakReference<PictureListActivity>) : AsyncTask<String, Unit, List<Photo>>() {
        private val mapper =
            jacksonObjectMapper().apply { configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) }

        override fun doInBackground(vararg params: String?): List<Photo> {
            return URL(params[0]).openConnection().run {
                connect()
                this as HttpsURLConnection
                if (responseCode / 100 != 2) {
                    Log.d("ASYNC_TASK", "Response code: $responseCode")
                    cancel(true)
                }
                inputStream.use {
                    mapper.readValue<List<Photo>>(it, object : TypeReference<List<Photo>>() {})
                }
            }
        }

        override fun onPostExecute(result: List<Photo>?) {
            if (result == null) return
            Log.d("ASYNC_TASK", "Result size: ${result.size}")
            activityRef.get()?.setupRecyclerView(result)
        }
    }

    class SimpleItemRecyclerViewAdapter(
        private val parentActivity: PictureListActivity,
        private val values: List<Photo>,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as Photo
                if (twoPane) {
                    val fragment = PictureDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(PictureDetailFragment.ARG_ITEM_ID, item.urls.regular)
                        }
                    }
                    parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.picture_detail_container, fragment)
                        .commit()
                } else {
                    val intent = Intent(v.context, PictureDetailActivity::class.java).apply {
                        putExtra(PictureDetailFragment.ARG_ITEM_ID, item.urls.regular)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.picture_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = item.id
            val desc = item.description?.let { "$it by " } ?: ""
            holder.contentView.text = "$desc${item.user.name} has ${item.likes} likes"

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.id_text
            val contentView: TextView = view.content
        }
    }
}
