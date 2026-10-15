//app\src\main\java\com\angonogiants\application\LeaderboardAdapter.java
package com.angonogiants.application;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class LeaderboardAdapter extends BaseAdapter {
    private Context context;
    private List<LeaderboardEntry> leaderboardList;

    public LeaderboardAdapter(Context context, List<LeaderboardEntry> leaderboardList) {
        this.context = context;
        this.leaderboardList = leaderboardList;
    }

    @Override
    public int getCount() {
        return leaderboardList.size();
    }

    @Override
    public Object getItem(int position) {
        return leaderboardList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.leaderboard_item, parent, false);
        }

        TextView playerNameView = convertView.findViewById(R.id.playerName);
        TextView playerScoreView = convertView.findViewById(R.id.playerScore);
        TextView wavesSurvivedView = convertView.findViewById(R.id.wavesSurvived);
        TextView timePlayedView = convertView.findViewById(R.id.timePlayed);

        ImageView medalGold = convertView.findViewById(R.id.medalgold);
        ImageView medalSilver = convertView.findViewById(R.id.medalzilver);
        ImageView medalBronze = convertView.findViewById(R.id.medalbronze);
        LinearLayout medalContainer = convertView.findViewById(R.id.medalContainer);
        LinearLayout playerInfoContainer = convertView.findViewById(R.id.playerInfoContainer);

        LeaderboardEntry entry = leaderboardList.get(position);

        playerNameView.setText("Player: " + entry.playerName);
        playerScoreView.setText("Score: " + entry.score);
        wavesSurvivedView.setText("Waves: " + entry.wavesSurvived);
        timePlayedView.setText("Time: " + entry.timePlayed);

        // Reset visibility
        medalGold.setVisibility(View.GONE);
        medalSilver.setVisibility(View.GONE);
        medalBronze.setVisibility(View.GONE);
        medalContainer.setVisibility(View.GONE);

        // Show medals for top 3 players
        if (position == 0) {
            medalContainer.setVisibility(View.VISIBLE);
            medalGold.setVisibility(View.VISIBLE);
        } else if (position == 1) {
            medalContainer.setVisibility(View.VISIBLE);
            medalSilver.setVisibility(View.VISIBLE);
        } else if (position == 2) {
            medalContainer.setVisibility(View.VISIBLE);
            medalBronze.setVisibility(View.VISIBLE);
        }

        // Adjust text alignment (center if no medal)
        if (position >= 3) {
            playerInfoContainer.setGravity(Gravity.CENTER);
        } else {
            playerInfoContainer.setGravity(Gravity.START);
        }

        return convertView;
    }



}
