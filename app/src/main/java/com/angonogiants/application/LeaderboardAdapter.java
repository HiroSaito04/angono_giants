
package com.angonogiants.application;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<LeaderboardEntry> entries;

    public LeaderboardAdapter(
            Context context,
            List<LeaderboardEntry> entries
    ) {
        this.inflater = LayoutInflater.from(context);
        this.entries = entries;
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public LeaderboardEntry getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static final class ViewHolder {
        TextView playerName;
        TextView highestLevel;
        TextView playerScore;
        TextView wavesSurvived;
        TextView timePlayed;

        ImageView medalGold;
        ImageView medalSilver;
        ImageView medalBronze;

        FrameLayout medalContainer;
        LinearLayout playerInfoContainer;

        ViewHolder(View view) {
            playerName =
                    view.findViewById(R.id.playerName);

            highestLevel =
                    view.findViewById(R.id.highestLevel);

            playerScore =
                    view.findViewById(R.id.playerScore);

            wavesSurvived =
                    view.findViewById(R.id.wavesSurvived);

            timePlayed =
                    view.findViewById(R.id.timePlayed);

            medalGold =
                    view.findViewById(R.id.medalgold);

            medalSilver =
                    view.findViewById(R.id.medalzilver);

            medalBronze =
                    view.findViewById(R.id.medalbronze);

            medalContainer =
                    view.findViewById(R.id.medalContainer);

            playerInfoContainer =
                    view.findViewById(R.id.playerInfoContainer);
        }
    }

    @Override
    public View getView(
            int position,
            View convertView,
            ViewGroup parent
    ) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(
                    R.layout.leaderboard_item,
                    parent,
                    false
            );

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        LeaderboardEntry entry = getItem(position);

        String name = entry.playerName == null
                || entry.playerName.trim().isEmpty()
                ? "Unknown Player"
                : entry.playerName;

        String displayName = position < 3
                ? "Player: " + name
                : "#" + (position + 1) + " - " + name;

        holder.playerName.setText(displayName);

        holder.highestLevel.setText(
                "Highest Level: " + entry.highestLevel
        );

        holder.playerScore.setText(
                String.format(
                        Locale.US,
                        "Best Score: %,d",
                        entry.score
                )
        );

        holder.wavesSurvived.setText(
                String.format(
                        Locale.US,
                        "Waves: %d",
                        entry.wavesSurvived
                )
        );

        holder.timePlayed.setText(
                "Time: " + (
                        entry.timePlayed == null
                        ? "Not recorded"
                        : entry.timePlayed
                )
        );

        // Reset recycled row states.
        holder.medalContainer.setVisibility(View.GONE);
        holder.medalGold.setVisibility(View.GONE);
        holder.medalSilver.setVisibility(View.GONE);
        holder.medalBronze.setVisibility(View.GONE);

        switch (position) {
            case 0:
                holder.medalContainer.setVisibility(
                        View.VISIBLE
                );
                holder.medalGold.setVisibility(View.VISIBLE);
                break;

            case 1:
                holder.medalContainer.setVisibility(
                        View.VISIBLE
                );
                holder.medalSilver.setVisibility(View.VISIBLE);
                break;

            case 2:
                holder.medalContainer.setVisibility(
                        View.VISIBLE
                );
                holder.medalBronze.setVisibility(View.VISIBLE);
                break;

            default:
                break;
        }

        holder.playerInfoContainer.setGravity(
                position < 3
                        ? Gravity.START | Gravity.CENTER_VERTICAL
                        : Gravity.CENTER
        );

        return convertView;
    }
}
