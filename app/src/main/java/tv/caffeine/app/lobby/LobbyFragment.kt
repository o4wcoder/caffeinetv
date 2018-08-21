package tv.caffeine.app.lobby


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import dagger.android.support.DaggerFragment
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.fragment_lobby.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.R
import javax.inject.Inject

class LobbyFragment : DaggerFragment() {

    @Inject lateinit var lobby: Lobby

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("ACCESS_TOKEN")?.run { loadLobby(this) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lobby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lobby_recycler_view.layoutManager = LinearLayoutManager(context)
    }

    private fun loadLobby(accessToken: String) {
        lobby.lobby("Bearer $accessToken").enqueue(object: Callback<LobbyResult?> {
            override fun onFailure(call: Call<LobbyResult?>?, t: Throwable?) {
                Timber.e(t, "Failed to get lobby")
            }

            override fun onResponse(call: Call<LobbyResult?>?, response: Response<LobbyResult?>?) {
                Timber.d("Success! Got lobby! ${response?.body()}")
                response?.body()?.cards?.run {
                    lobby_recycler_view.adapter = LobbyAdapter(this)
                }
            }
        })
    }

}

class LobbyAdapter(val cards: Array<LobbyCard>) : RecyclerView.Adapter<LobbyCardVH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LobbyCardVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.broadcast_card, parent, false)
        return LobbyCardVH(view)
    }

    override fun getItemCount(): Int = cards.size

    override fun onBindViewHolder(holder: LobbyCardVH, position: Int) {
        val card = cards[position]
        val previewImageUrl = "https://images.caffeine.tv${card.broadcast.previewImagePath}"
        Picasso.get()
                .load(previewImageUrl)
                .transform(RoundedCornersTransformation(40, 0)) // TODO: multiply by display density
                .into(holder.previewImageView)
        Timber.d("Preview image: ${previewImageUrl}")
        val avatarImageUrl = "https://images.caffeine.tv${card.broadcast.user.avatarImagePath}"
        Picasso.get()
                .load(avatarImageUrl)
                .transform(CropCircleTransformation())
                .into(holder.avatarImageView)
        Timber.d("Avatar image: ${avatarImageUrl}")
        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.caffeine.tv/${card.broadcast.user.username}"))
            startActivity(holder.itemView.context, intent, null)
        }
    }

}

class LobbyCardVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val previewImageView: ImageView = itemView.findViewById(R.id.preview_image_view)
    val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
}
