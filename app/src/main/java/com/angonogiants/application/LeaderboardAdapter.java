//app/src/main/java/com/angonogiants/application/LeaderboardAdapter.java
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
import java.util.Locale;

public class LeaderboardAdapter extends BaseAdapter {

    private final Context context;

    private final List<LeaderboardEntry> leaderboardList;

    public LeaderboardAdapter(
            Context context,
            List<LeaderboardEntry> leaderboardList
    ) {
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
    public View getView(
            int position,
            View convertView,
            ViewGroup parent
    ) {
        if (convertView == null) {
            convertView = LayoutInflater
                    .from(context)
                    .inflate(
                            R.layout.leaderboard_item,
                            parent,
                            false
                    );
        }

        TextView playerNameView =
                convertView.findViewById(
                        R.id.playerName
                );

        TextView playerLevelView =
                convertView.findViewById(
                        R.id.playerLevel
                );

        TextView playerScoreView =
                convertView.findViewById(
                        R.id.playerScore
                );

        TextView wavesSurvivedView =
                convertView.findViewById(
                        R.id.wavesSurvived
                );

        TextView timePlayedView =
                convertView.findViewById(
                        R.id.timePlayed
                );

        ImageView medalGold =
                convertView.findViewById(
                        R.id.medalgold
                );

        ImageView medalSilver =
                convertView.findViewById(
                        R.id.medalzilver
                );

        ImageView medalBronze =
                convertView.findViewById(
                        R.id.medalbronze
                );

        LinearLayout medalContainer =
                convertView.findViewById(
                        R.id.medalContainer
                );

        LinearLayout playerInfoContainer =
                convertView.findViewById(
                        R.id.playerInfoContainer
                );

        LeaderboardEntry entry =
                leaderboardList.get(position);

        String formattedScore = String.format(
                Locale.US,
                "%,d",
                entry.score
        );

        playerNameView.setText(
                "#" + (position + 1)
                        + "  " + entry.playerName
        );

        playerLevelView.setText(
                "LEVEL " + entry.highestLevel
        );

        playerScoreView.setText(
                "SCORE: " + formattedScore
        );

        wavesSurvivedView.setText(
                "WAVES: " + entry.wavesSurvived
        );

        timePlayedView.setText(
                "TIME: " + entry.timePlayed
        );

        // Always reset visibility for recycled rows.
        medalGold.setVisibility(View.GONE);
        medalSilver.setVisibility(View.GONE);
        medalBronze.setVisibility(View.GONE);
        medalContainer.setVisibility(View.GONE);

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

        if (position < 3) {
            playerInfoContainer.setGravity(
                    Gravity.CENTER_VERTICAL
            );
        } else {
            playerInfoContainer.setGravity(
                    Gravity.CENTER
            );
        }

        return convertView;
    }
}
