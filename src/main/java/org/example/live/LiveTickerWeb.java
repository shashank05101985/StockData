package org.example.live;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnTicks;
import org.example.model.PreviousDayData;
import org.example.model.StockMomentum;
import org.example.util.TokenUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

public class LiveTickerWeb {

    private static KiteTicker liveTicker;

    public static void startWebSocket(KiteConnect kite, java.util.Set<String> tokens) throws KiteException, Exception {

        Map<Long, String> tokenMap = TokenUtils.getTokens(kite, tokens);
        System.out.println("TokeMap Size " + tokenMap.size());
        liveTicker = new KiteTicker(kite.getAccessToken(), kite.getApiKey());

        liveTicker.setTryReconnection(true);
        liveTicker.setMaximumRetries(10);
        liveTicker.setMaximumRetryInterval(30);


        // =====================================================
        // CONNECT
        // =====================================================

        liveTicker.setOnConnectedListener(new OnConnect() {

            @Override
            public void onConnected() {

                System.out.println("WebSocket Connected");

                ArrayList<Long> instrumentTokens = new ArrayList<>(tokenMap.keySet());

                liveTicker.subscribe(instrumentTokens);

                /*
                 * FULL mode is required because
                 * we need market depth.
                 */
                liveTicker.setMode(instrumentTokens, KiteTicker.modeFull);
            }
        });


        // =====================================================
        // TICKS
        // =====================================================

        liveTicker.setOnTickerArrivalListener(new OnTicks() {

            @Override
            public void onTicks(ArrayList<Tick> ticks) {
                for (Tick tick : ticks) {

                    try {
                        String symbol = tokenMap.get(tick.getInstrumentToken());
                        if (symbol == null) {
                            continue;
                        }
                        RealtimeMomentumEngine.onTick(symbol, tick);

                    } catch (Exception e) {

                        e.printStackTrace();
                    }
                }
            }
        });


        liveTicker.connect();
    }

}
