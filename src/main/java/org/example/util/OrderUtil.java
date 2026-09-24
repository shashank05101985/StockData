package org.example.util;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;

import java.io.IOException;

public class OrderUtil {

    static final String API_KEY = "8311x4p8tm56j4vc";
    static final String ACCESS_TOKEN = "C69NHmiqUf3qCtQdP80UEPAd1MjSwBpv";

    public static void placeBuyOrder(String symbol, double price, int quantity) {
        KiteConnect kite = new KiteConnect(API_KEY);
        kite.setAccessToken(ACCESS_TOKEN);
        /** Place order method requires a orderParams argument which contains,
         * tradingsymbol, exchange, transaction_type, order_type, quantity, product, price, trigger_price, disclosed_quantity, validity
         * squareoff_value, stoploss_value, trailing_stoploss
         * and variety (value can be regular, bo, co, amo)
         * place order will return order model which will have only orderId in the order model
         *
         * Following is an example param for LIMIT order,
         * if a call fails then KiteException will have error message in it
         * Success of this call implies only order has been placed successfully, not order execution. */
        try {
            OrderParams orderParams = new OrderParams();
            orderParams.quantity = quantity;
            orderParams.orderType = Constants.ORDER_TYPE_LIMIT;
            orderParams.tradingsymbol = symbol;
            orderParams.product = Constants.PRODUCT_CNC;
            orderParams.exchange = Constants.EXCHANGE_NSE;
            orderParams.transactionType = Constants.TRANSACTION_TYPE_BUY;
            orderParams.validity = Constants.VALIDITY_DAY;
            orderParams.price = price;
            orderParams.triggerPrice = 0.0;
            orderParams.marketProtection = 2;
            orderParams.tag = "myTag"; //tag is optional and it cannot be more than 8 characters and only alphanumeric is allowed
            Order order = kite.placeOrder(orderParams, Constants.VARIETY_REGULAR);
            System.out.println(order.orderId);
        } catch (Exception | KiteException e) {
            System.out.println("========== ORDER ERROR ==========");
            System.out.println("Class   : " + e.getClass().getName());
            System.out.println("Message : " + e.getMessage());
            System.out.println("=================================");

            e.printStackTrace();
        }
    }
    public static void placeSellOrder(String symbol, double price, int quantity) {
        /** Place order method requires a orderParams argument which contains,
         * tradingsymbol, exchange, transaction_type, order_type, quantity, product, price, trigger_price, disclosed_quantity, validity
         * squareoff_value, stoploss_value, trailing_stoploss
         * and variety (value can be regular, bo, co, amo)
         * place order will return order model which will have only orderId in the order model
         *
         * Following is an example param for LIMIT order,
         * if a call fails then KiteException will have error message in it
         * Success of this call implies only order has been placed successfully, not order execution. */
        KiteConnect kite = new KiteConnect(API_KEY);
        kite.setAccessToken(ACCESS_TOKEN);
        try {
            OrderParams orderParams = new OrderParams();
            orderParams.quantity = quantity;
            orderParams.orderType = Constants.ORDER_TYPE_LIMIT;
            orderParams.tradingsymbol = symbol;
            orderParams.product = Constants.PRODUCT_CNC;
            orderParams.exchange = Constants.EXCHANGE_NSE;
            orderParams.transactionType = Constants.TRANSACTION_TYPE_SELL;
            orderParams.validity = Constants.VALIDITY_DAY;
            orderParams.price = price;
            orderParams.triggerPrice = 0.0;
            orderParams.marketProtection = 2;
            orderParams.tag = "myTag"; //tag is optional and it cannot be more than 8 characters and only alphanumeric is allowed
            Order order = kite.placeOrder(orderParams, Constants.VARIETY_REGULAR);
            System.out.println(order.orderId);
        } catch (Exception | KiteException e) {
            System.out.println("========== ORDER ERROR ==========");
            System.out.println("Class   : " + e.getClass().getName());
            System.out.println("Message : " + e.getMessage());
            System.out.println("=================================");

            e.printStackTrace();
        }
    }
}
