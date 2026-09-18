package org.example.util;


import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;

public class TokenUtils {

    static String[] momentumStocks = {
        "ABB",
        "ABCAPITAL",
        "ACC",
        "ACE",
        "ACL",
        "ADANIENPP-E1",
        "ADANIENSOL",
        "ADANIENT",
        "ADANIGREEN",
        "ADANIPORTS",
        "ADSL",
        "ADVENZYMES",
        "AEQUS",
        "AEROFLEX",
        "AKUMS",
        "ALOKINDS",
        "AMBER",
        "AMBUJACEM",
        "APARINDS",
        "APOLLO",
        "APOLLOHOSP",
        "ASIANPAINT",
        "ASHOKLEY",
        "ATALREAL",
        "ATULAUTO",
        "AVANTEL",
        "AWL",
        "BAJAJ-AUTO",
        "BAJAJFINANCE",
        "BAJAJFINSV",
        "BAJAJHFL",
        "BALUFORGE",
        "BANDHANBNK",
        "BANKBARODA",
        "BANKINDIA",
        "BEL",
        "BEML",
        "BERGEPAINT",
        "BHARTIARTL",
        "BHEL",
        "BIOCON",
        "BLS",
        "BLSE",
        "BLUECLOUDS",
        "BPCL",
        "BRITANNIA",
        "BSOFT",
        "BSE",
        "CAMS",
        "CANBK",
        "CDSL",
        "CENTENKA",
        "CENTRALBK",
        "CENTRUM",
        "CHAMBLFERT",
        "CIPLA",
        "COALINDIA",
        "COCHINSHIP",
        "COFORGE",
        "COLPAL",
        "CYBERTECH",
        "DABUR",
        "DALBHARAT",
        "DBREALTY",
        "DEEPAKNTR",
        "DELHIVERY",
        "DELTACORP",
        "DIXON",
        "DLF",
        "DRREDDY",
        "DREAMFOLKS",
        "EASEMYTRIP",
        "EDELWEISS",
        "EICHERMOT",
        "EIEL",
        "ELGIEQUIP",
        "EMBDL-T",
        "EMSLIMITED",
        "ETERNAL",
        "EXICOM",
        "FCSSOFT",
        "FEDERALBNK",
        "FIVESTAR",
        "GAIL",
        "GMRAIRPORT",
        "GMBREW",
        "GNFC",
        "GODREJCP",
        "GODREJPROP",
        "GRASIM",
        "GRAPHITE",
        "GRSE",
        "GTLINFRA",
        "GUJTLRM",
        "GSFC",
        "GSPL",
        "HAPPSTMNDS",
        "HCLTECH",
        "HDBFS",
        "HDFCBANK",
        "HDFCLIFE",
        "HEMIPROP",
        "HEROMOTOCO",
        "HFCL",
        "HIKAL",
        "HINDALCO",
        "HINDPETRO",
        "HINDUNILVR",
        "HUDCO",
        "HYUNDAI",
        "IBREALEST",
        "IBULLSLTD",
        "ICICIBANK",
        "IDEA",
        "IDEAFORGE",
        "IDBI",
        "IEX",
        "IFCI",
        "IFBIND",
        "IGIL",
        "INDIAMART",
        "INDIGO",
        "INDUSINDBK",
        "INDUSTOWER",
        "INFIBEAM",
        "INFY",
        "INOXGREEN",
        "INOXWIND",
        "IOB",
        "IOC",
        "IRB",
        "IRBINFRA",
        "IRCON",
        "IRCTC",
        "IREDA",
        "IRFC",
        "ITC",
        "ITCHOTELS",
        "J&KBANK",
        "JINDALSAW",
        "JINDALSTEL",
        "JIOFIN",
        "JPASSOCIAT-T",
        "JPPOWER",
        "JSWSTEEL",
        "JUSTDIAL",
        "JWL",
        "KALYANKJIL",
        "KAYNES",
        "KEI",
        "KFINTECH",
        "KIRIINDUS",
        "KIMS",
        "KOTAKBANK",
        "KPITTECH",
        "KPT-X",
        "KTKBANK",
        "LATENTVIEW",
        "LICI",
        "LT",
        "LTIM",
        "LTTS",
        "MAHABANK",
        "MANAPPURAM",
        "MANGIND-XT",
        "MAPMYINDIA",
        "MARICO",
        "MARUTI",
        "M&M",
        "MAZDOCK",
        "MCX",
        "MEDANTA",
        "MMTC",
        "MOIL",
        "MOTILALOFS",
        "MTNL",
        "MPHASIS",
        "MUFTI",
        "MUTHOOTFIN",
        "NAUKRI",
        "NBCC",
        "NCC",
        "NATIONALUM",
        "NESTLEIND",
        "NETWEB",
        "NETWORK18",
        "NHPC",
        "NMDC",
        "NTPC",
        "NOVAAGRI",
        "NURECA",
        "OIL",
        "OLAELEC",
        "ONGC",
        "PARACABLES",
        "PATELENG",
        "PAYTM",
        "PAYTMONEY",
        "PFC",
        "PGEL",
        "PIIND",
        "PIRAMALFIN",
        "PNB",
        "PNBGILTS",
        "PNBHOUSING",
        "PNCINFRA",
        "POLICYBZR",
        "POLYCAB",
        "POLYPLEX",
        "POWERGRID",
        "POWERINDIA",
        "PREMEXPLN",
        "PREMIERENE",
        "PVRINOX",
        "RAILTEL",
        "RAIN",
        "RATEGAIN",
        "RCF",
        "RECLTD",
        "REFEX",
        "RELIANCE",
        "RENUKA",
        "RBLBANK",
        "ROUTE",
        "RPOWER",
        "RVHL",
        "RVNL",
        "SAGILITY",
        "SAIL",
        "SAKSOFT",
        "SAMMAANCAP",
        "SBICARD",
        "SBILIFE",
        "SBIN",
        "SDBL",
        "SHAREINDIA",
        "SHAKTIPUMP",
        "SHREECEM",
        "SHRIRAMFIN",
        "SIGACHI",
        "SJVN",
        "SOLARWORLD",
        "SONACOMS",
        "SOUTHBANK",
        "SPICEJET",
        "STARHEALTH",
        "STLTECH",
        "SUBEX",
        "SUZLON",
        "SWSOLAR",
        "SWANCORP",
        "TANLA",
        "TATAELXSI",
        "TATACHEM",
        "TATACONSUM",
        "TATAINVEST",
        "TATAMOTORS",
        "TATAPOWER",
        "TATASTEEL",
        "TATATECH",
        "TCS",
        "TDPOWERSYS",
        "TECHM",
        "TEJASNET",
        "TEMBO",
        "TFCILTD",
        "TITAGARH",
        "TITAN",
        "TMCV",
        "TMPV",
        "TRENT",
        "TRIDENT",
        "TRIVENI",
        "TVSMOTOR",
        "TVSSCS",
        "UCOBANK",
        "UJJIVANSFB",
        "ULTRACEMCO",
        "UNIMECH",
        "UPL",
        "VARROC",
        "VARDHACRLC",
        "VEDL",
        "VMM",
        "VPRPL",
        "WAAREEENER",
        "WIPRO",
        "WPIL",
        "WSI",
        "YESBANK",
        "ZEE",
        "ZENTEC"
    };

    static List<Instrument> instruments = null;

    public static Map<Long, String> getTokens(
        KiteConnect kite,
        Set<String> symbolSet)
        throws Exception, KiteException {

        if (CollectionUtils.isEmpty(instruments)) {

            instruments =
                kite.getInstruments("NSE");
        }

        List<Instrument> bseInstruments =
            kite.getInstruments("BSE");

        Map<Long, String> tokenMap =
            new LinkedHashMap<>();


        // =====================================================
        // FIRST: NSE
        // =====================================================

        Set<String> nseSymbols =
            new HashSet<>();

        for (Instrument i : instruments) {

            if ("EQ".equals(i.instrument_type)
                && symbolSet.contains(i.tradingsymbol)) {

                tokenMap.put(
                    i.instrument_token,
                    i.tradingsymbol
                );

                nseSymbols.add(
                    i.tradingsymbol
                );
            }
        }


        // =====================================================
        // SECOND: BSE ONLY IF NOT IN NSE
        // =====================================================

        for (Instrument i : bseInstruments) {

            if (!"EQ".equals(i.instrument_type)) {
                continue;
            }

            if (!symbolSet.contains(i.tradingsymbol)) {
                continue;
            }

            // NSE already has this symbol
            if (nseSymbols.contains(i.tradingsymbol)) {
                continue;
            }

            // Symbol exists only in BSE
            tokenMap.put(
                i.instrument_token,
                i.tradingsymbol
            );
        }


        return tokenMap;
    }

    public static Set<String> getTokens()
    {
        return new HashSet<>(List.of(momentumStocks));
    }
}
