package lavalink.server.extsource.spotilava;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import lavalink.server.config.ServerConfig;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import org.slf4j.LoggerFactory;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import java.net.URI;
/**
 * Audio track that handles processing SpotiLava tracks.
 */
public class SpotiLavaAudioTrack extends DelegatedAudioTrack {
    private static final Logger log = LoggerFactory.getLogger(SpotiLavaAudioTrack.class);
    private final SpotiLavaSourceManager sourceManager;
    /**
    * @param trackInfo Track info
    * @param sourceManager Source manager which was used to find this track
    */
    public SpotiLavaAudioTrack(AudioTrackInfo trackInfo, SpotiLavaSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

     @Override
     public void process(LocalAudioTrackExecutor localExecutor) throws Exception {
         try (HttpInterface httpInterface = sourceManager.getHttpInterface()) {
             if (sourceManager.spotiLavaUrl == null) {
                 log.warn("SpotiLavaSource is enabled but no spotiLavaUrl set, returning as null");
                 throw new Exception("SpotiLavaSource is enabled but no spotiLavaUrl set, but trying to playing track");
             }
             String playbackUrl = "https://" + sourceManager.spotiLavaUrl + "/" + trackInfo.identifier + "/listen";
             log.debug("Starting Spotify track from URL: {}", playbackUrl);
             try (PersistentHttpStream stream = new PersistentHttpStream(httpInterface, new URI(playbackUrl), null)) {
                 processDelegate(new OggAudioTrack(trackInfo, stream), localExecutor);
            }
        }
    }


    @Override
    protected AudioTrack makeShallowClone() {
        return new SpotiLavaAudioTrack(trackInfo, sourceManager);
    }
    
    
    @Override
    public AudioSourceManager getSourceManager() {
        return sourceManager;
    }

}