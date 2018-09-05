package tv.caffeine.app.lobby


import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
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
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.Lobby
import tv.caffeine.app.api.LobbyCard
import tv.caffeine.app.api.LobbyResult
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject

class LobbyFragment : DaggerFragment() {

    @Inject lateinit var accountsService: AccountsService
    @Inject lateinit var lobby: Lobby
    @Inject lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lobby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).setSupportActionBar(lobby_toolbar)
        lobby_recycler_view.layoutManager = LinearLayoutManager(context)
        lobby_recycler_view.adapter = LobbyAdapter(arrayOf())
        loadLobby()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.lobby, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.profile -> {
                findNavController().navigate(R.id.profile)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadLobby() {
        lobby.lobby().enqueue(object: Callback<LobbyResult?> {
            override fun onFailure(call: Call<LobbyResult?>?, t: Throwable?) {
                Timber.e(t, "Failed to get lobby")
            }

            override fun onResponse(call: Call<LobbyResult?>?, response: Response<LobbyResult?>?) {
                Timber.d("Success! Got lobby! ${response?.body()}")
                lobby_recycler_view ?: return Timber.d("RecyclerView is null, exiting")
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
        val game = card.broadcast.game
        if (game != null) {
            val gameLogoImageUrl = "https://images.caffeine.tv${game.bannerImagePath}"
            Picasso.get()
                    .load(gameLogoImageUrl)
                    .into(holder.gameLogoImageView)
        } else {
            holder.gameLogoImageView.setImageDrawable(null)
        }
        holder.usernameTextView.text = card.broadcast.user.username
        if (card.broadcast.user.isVerified) {
            holder.usernameTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.verified_large, 0)
        } else {
            holder.usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        }
        holder.broadcastTitleTextView.text = card.broadcast.name
        holder.itemView.setOnClickListener {
            // TODO: switch to safeargs when their code gen is fixed
//            val action = LobbyFragmentDirections.actionLobbyFragmentToStage(card.broadcast.user.stageId, card.broadcast.user.username)
//            Navigation.findNavController(holder.itemView).navigate(action)
            val args = Bundle().apply {
                putString("stageIdentifier", card.broadcast.user.stageId)
                putString("broadcaster", card.broadcast.user.username)
            }
            Navigation.findNavController(holder.itemView).navigate(R.id.stage, args)
        }
    }

}

class LobbyCardVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val previewImageView: ImageView = itemView.findViewById(R.id.preview_image_view)
    val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    val gameLogoImageView: ImageView = itemView.findViewById(R.id.game_logo_image_view)
    val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    val broadcastTitleTextView: TextView = itemView.findViewById(R.id.broadcast_title_text_view)
}
