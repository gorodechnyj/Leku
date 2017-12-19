package com.schibstedspain.leku;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gorodechnyj
 * Created at 04.06.17.
 */

public class StringUtils {

    @NonNull
    public static String joinNullable(CharSequence delimiter, String... tokens) {
        List<String> nonNullTokens = getNonNullTokens(tokens);

        if (nonNullTokens.size() > 0) {
            StringBuilder sb = new StringBuilder();
            boolean firstTime = true;
            for (Object token : nonNullTokens) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    sb.append(delimiter);
                }
                sb.append(token);
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    @NonNull
    public static String joinNullable(CharSequence delimiter, Iterable<String> tokens) {
        List<String> nonNullTokens = getNonNullTokens(tokens);

        if (nonNullTokens.size() > 0) {
            StringBuilder sb = new StringBuilder();
            boolean firstTime = true;
            for (Object token : nonNullTokens) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    sb.append(delimiter);
                }
                sb.append(token);
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    private static List<String> getNonNullTokens(String... tokens) {
        List<String> nonNullTokens = new ArrayList<>();
        for (String token : tokens) {
            if (!TextUtils.isEmpty(token)) {
                nonNullTokens.add(token);
            }
        }
        return nonNullTokens;
    }

    private static List<String> getNonNullTokens(Iterable<String> tokens) {
        List<String> nonNullTokens = new ArrayList<>();
        for (String nextToken : tokens) {
            if (!TextUtils.isEmpty(nextToken)) {
                nonNullTokens.add(nextToken);
            }
        }
        return nonNullTokens;
    }
}
