package stream.flarebot.flarebot;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import stream.flarebot.flarebot.database.DatabaseManager;
import stream.flarebot.flarebot.objects.GuildWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.PreparedStatement;

public class GuildSaveListener implements RemovalListener<String, GuildWrapper> {

    @Override
    public void onRemoval(@Nullable String key, @Nullable GuildWrapper value, @Nonnull RemovalCause cause) {
        DatabaseManager.run(conn -> {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO guilds (guild_id, guild_data) VALUES (?, ?) " +
                    "ON CONFLICT (guild_id) DO UPDATE SET guild_data = VALUES(guild_data)");
            ps.setString(1, key);
            ps.setString(2, DataHandler.gson.toJson(value));
            ps.executeUpdate();
        });
    }
}
