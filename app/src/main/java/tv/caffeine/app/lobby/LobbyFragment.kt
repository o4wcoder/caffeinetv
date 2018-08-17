package tv.caffeine.app.lobby


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.fragment_lobby.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tv.caffeine.app.R

class LobbyFragment : Fragment() {

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
        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        val gsonConverterFactory = GsonConverterFactory.create(gson)
        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.caffeine.tv")
                .addConverterFactory(gsonConverterFactory)
                .build()
        val lobby = retrofit.create(Lobby::class.java)
        lobby.lobby("Bearer $accessToken").enqueue(object: Callback<LobbyResult?> {
            override fun onFailure(call: Call<LobbyResult?>?, t: Throwable?) {
                Log.e("API: Lobby", "Failed to get lobby", t)
            }

            override fun onResponse(call: Call<LobbyResult?>?, response: Response<LobbyResult?>?) {
                Log.d("API: Lobby", "Success! Got lobby! ${response?.body()}")
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
        Log.d("LobbyAdapter", "Preview image: ${previewImageUrl}")
        val avatarImageUrl = "https://images.caffeine.tv${card.broadcast.user.avatarImagePath}"
        Picasso.get()
                .load(avatarImageUrl)
                .transform(CropCircleTransformation())
                .into(holder.avatarImageView)
        Log.d("LobbyAdapter", "Avatar image: ${avatarImageUrl}")
    }

}

class LobbyCardVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val previewImageView: ImageView = itemView.findViewById(R.id.preview_image_view)
    val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
}
