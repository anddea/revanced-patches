package app.revanced.extension.shared.innertube.utils.javatube;

import android.annotation.TargetApi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.revanced.extension.shared.utils.Logger;

/**
 * The functions used in this class are referenced below:
 * <a href="https://github.com/felipeucelli/JavaTube/blob/ec9011fa2ed584b867d276e683c421059b87bec5/src/main/java/com/github/felipeucelli/javatube/Cipher.java">JavaTube</a>
 */
@TargetApi(26)
public class Cipher {
    private final String[] INITIAL_FUNCTION_REGEXES = {
            "(?<sig>[a-zA-Z0-9_$]+)\\s*=\\s*function\\(\\s*(?<arg>[a-zA-Z0-9_$]+)\\s*\\)\\s*\\{\\s*(\\k<arg>)\\s*=\\s*(\\k<arg>)\\.split\\(\\s*[a-zA-Z0-9_\\$\\\"\\[\\]]+\\s*\\)\\s*;\\s*[^}]+;\\s*return\\s+(\\k<arg>)\\.join\\(\\s*[a-zA-Z0-9_\\$\\\"\\[\\]]+\\s*\\)",
            "\\b(?<var>[a-zA-Z0-9_$]+)&&\\((\\k<var>)=(?<sig>[a-zA-Z0-9_$]{2,})\\(decodeURIComponent\\((\\k<var>)\\)\\)",
            "(?:\\b|[^a-zA-Z0-9_$])(?<sig>[a-zA-Z0-9_$]{2,})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\\\"\\\"\\s*\\)(?:;[a-zA-Z0-9_$]{2}\\.[a-zA-Z0-9_$]{2}\\(a,\\d+\\))?"
    };
    // TODO: Regular expressions need to be updated.
    private final String THROTTLING_FUNCTION_REGEX = """
                            (?x)
                                (?:
                                    \\.get\\(\\"n\\"\\)\\)&&\\(b=|
                                    (?:
                                        b=String\\.fromCharCode\\(110\\)|
                                        (?<stridx>[a-zA-Z0-9_$.]+)&&\\(b=\\"nn\\"\\[\\+(\\k<stridx>)\\]
                                    )
                                    (?:
                                        ,[a-zA-Z0-9_$]+\\(a\\))?,c=a\\.
                                        (?:
                                            get\\(b\\)|
                                            [a-zA-Z0-9_$]+\\[b\\]\\|\\|null
                                        )\\)&&\\(c=|
                                    \\b(?<var>[a-zA-Z0-9_$]+)=
                                )(?<nfunc>[a-zA-Z0-9_$]+)(?:\\[(?<idx>\\d+)\\])?\\([a-zA-Z]\\)
                                ((var)|,[a-zA-Z0-9_$]+\\.set\\((?:\\"n+\\"|[a-zA-Z0-9_$]+)\\,(\\k<var>)\\))""";

    private final String playerJsUrl;
    private final JsInterpreter jsInterpreter;
    private final String signatureFunctionName;
    private final String throttlingFunctionName;

    public Cipher(@NonNull String playerJs, @NonNull String playerJsUrl) {
        this.playerJsUrl = playerJsUrl;
        jsInterpreter = new JsInterpreter(playerJs);
        signatureFunctionName = getInitialFunctionName(playerJs);
        throttlingFunctionName = getThrottlingFunctionName(playerJs);
    }

    private String getInitialFunctionName(@NonNull String playerJs) {
        for (String pattern : INITIAL_FUNCTION_REGEXES){
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(playerJs);
            if (matcher.find()) {
                return matcher.group("sig");
            }
        }
        Logger.printDebug(() -> "getInitialFunctionName: Could not find function name in playerJs");
        return "";
    }

    private String getThrottlingFunctionName(@NonNull String playerJs) {
        try {
            String[] globalVar = jsInterpreter.extractPlayerJsGlobalVar(playerJs);
            String name = globalVar[0];
            String code = globalVar[1];
            String value = globalVar[2];
            if (code != null) {
                Object array = jsInterpreter.interpretExpression(value, new LocalNameSpace(new HashMap<>()), 100);
                if (array instanceof ArrayList<?>){
                    @SuppressWarnings("unchecked")
                    ArrayList<String> globalArray = (ArrayList<String>) array;
                    for (int i = 0; i < globalArray.size(); i ++){
                        if (globalArray.get(i).endsWith("_w8_")){
                            String pattern =
                                    "(?xs)"
                                            + "[;\\n](?:"
                                            + "(?<f>function\\s+)|"
                                            + "(?:var\\s+)?"
                                            + ")(?<funcname>[a-zA-Z0-9_$]+)\\s*((f)|=\\s*function\\s*)"
                                            + "\\((?<argname>[a-zA-Z0-9_$]+)\\)\\s*\\{"
                                            + "(?:(?!\\};(?![\\]\\)])).)+"
                                            + "\\}\\s*catch\\(\\s*[a-zA-Z0-9_$]+\\s*\\)\\s*"
                                            + "\\{\\s*return\\s+" + name + "\\[" + i + "\\]\\s*\\+\\s*(\\k<argname>)\\s*\\}\\s*return\\s+[^}]+\\}[;\\n]"
                                    ;
                            Pattern regex = Pattern.compile(pattern);
                            Matcher matcher = regex.matcher(playerJs);
                            if (matcher.find()){
                                return matcher.group("funcname");
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Pattern regex = Pattern.compile(THROTTLING_FUNCTION_REGEX);
            Matcher matcher = regex.matcher(playerJs);
            if (matcher.find()){
                String nFunc = matcher.group("nfunc");
                if (nFunc != null) {
                    String funName = Pattern.quote(nFunc);
                    String idx = matcher.group("idx");
                    if (StringUtils.isNotEmpty(idx)){
                        String pattern2 = "var " + funName + "\\s*=\\s*\\[(.+?)]";
                        Pattern regex2 = Pattern.compile(pattern2);
                        Matcher nFuncFound = regex2.matcher(playerJs);
                        if (nFuncFound.find()){
                            return nFuncFound.group(1);
                        }
                    }
                }
            }
            Logger.printDebug(() -> "getThrottlingFunctionName: Could not find function name in playerJsUrl: " + playerJsUrl);
        } catch (Exception ex) {
            Logger.printException(() -> "getThrottlingFunctionName: Could not find function name in playerJsUrl: " + playerJsUrl, ex);
        }
        return "";
    }

    @Nullable
    public String getSignature(@NonNull String cipherSignature) {
        try {
            return jsInterpreter.callFunction(signatureFunctionName, cipherSignature).toString();
        } catch (Exception ex) {
            Logger.printException(() -> "getNSig failed", ex);
        }
        return null;
    }

    @Nullable
    public String getNParam(@NonNull String n) {
        try {
            return jsInterpreter.callFunction(throttlingFunctionName, n).toString();
        } catch (Exception ex) {
            Logger.printException(() -> "getNSig failed", ex);
        }
        return null;
    }
}