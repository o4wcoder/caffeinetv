package tv.caffeine.app.lobby


import android.os.Bundle
import android.view.*
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
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
import tv.caffeine.app.auth.Accounts
import javax.inject.Inject

class LobbyFragment : DaggerFragment() {

    @Inject lateinit var accounts: Accounts
    @Inject lateinit var lobby: Lobby

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        arguments?.run {
            val accessToken = getString("ACCESS_TOKEN")
            val xCredential = getString("X_CREDENTIAL")
            loadLobby(accessToken, xCredential)
        }
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
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.lobby, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.profile -> {
                accounts.signOut().enqueue(object: Callback<Unit?> {
                    override fun onFailure(call: Call<Unit?>?, t: Throwable?) {
                        Timber.e(t, "Failed to sign out")
                    }

                    override fun onResponse(call: Call<Unit?>?, response: Response<Unit?>?) {
                        Timber.d("Signed out successfully $response")
                    }
                })
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadLobby(accessToken: String, xCredential: String) {
        lobby.lobby("Bearer $accessToken").enqueue(object: Callback<LobbyResult?> {
            override fun onFailure(call: Call<LobbyResult?>?, t: Throwable?) {
                Timber.e(t, "Failed to get lobby")
            }

            override fun onResponse(call: Call<LobbyResult?>?, response: Response<LobbyResult?>?) {
                Timber.d("Success! Got lobby! ${response?.body()}")
                response?.body()?.cards?.run {
                    lobby_recycler_view.adapter = LobbyAdapter(accessToken, xCredential, this)
                }
            }
        })
    }

}

class LobbyAdapter(val accessToken: String, val xCredential: String, val cards: Array<LobbyCard>) : RecyclerView.Adapter<LobbyCardVH>() {
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
            val args = Bundle()
            args.putString("STAGE_IDENTIFIER", card.broadcast.user.stageId)
            args.putString("ACCESS_TOKEN", accessToken)
            args.putString("X_CREDENTIAL", xCredential)
            args.putString("BROADCASTER", card.broadcast.user.username)
            Navigation.findNavController(holder.itemView).navigate(R.id.action_lobbyFragment_to_stage, args)
        }
    }

}

class LobbyCardVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val previewImageView: ImageView = itemView.findViewById(R.id.preview_image_view)
    val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
}
