package stream.flarebot.flarebot;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import stream.flarebot.flarebot.database.DatabaseManager;
import stream.flarebot.flarebot.objects.GuildWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.PreparedStatement;

public class GuildSaveListener implements RemovalListener<Long, GuildWrapper> {

    @Override
    public void onRemoval(@Nullable Long key, @Nullable GuildWrapper value, @Nonnull RemovalCause cause) {
        DatabaseManager.run(conn -> {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO guilds (guild_id, guild_data) VALUES (?, ?) " +
                    "ON CONFLICT (guild_id) DO UPDATE SET guild_data = ?");
            ps.setLong(1, key);
            String json = DataHandler.gson.toJson(value);
            ps.setString(2, json);
            ps.setString(3, json);
            ps.executeUpdate();
        });
    }
}
