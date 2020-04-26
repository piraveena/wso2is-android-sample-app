package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okio.Okio;


public class UserInfoActivity extends AppCompatActivity {

    String idToken;
    String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

    }

    @Override
    protected void onStart() {

        super.onStart();
        handleAuthorizationResponse(getIntent());
    }

    private void handleAuthorizationResponse(Intent intent) {

        final AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        String secret = "CShEPt6LUn3reIohVdSuRBE0eH4a";
        Map<String, String> additionalParameters = new HashMap<>();
        additionalParameters.put("client_secret", secret);

        AuthorizationException error = AuthorizationException.fromIntent(intent);
        final AuthState authState = new AuthState(response, error);
        AuthorizationService service = new AuthorizationService(this);
        performTokenRequest(service, response.createTokenExchangeRequest(additionalParameters), this::handleCodeExchangeResponse);
    }

    private void performTokenRequest(AuthorizationService authService, TokenRequest request,
                                     AuthorizationService.TokenResponseCallback callback) {

        String secret = "qrpIF2hh0bKl0Hojt4XTFuczy2oa";
        ClientAuthentication clientAuthentication = new ClientSecretBasic(secret);
        authService.performTokenRequest(request, clientAuthentication, callback);
    }


    private void handleCodeExchangeResponse(TokenResponse tokenResponse, AuthorizationException authException) {


        idToken = tokenResponse.idToken;
        accessToken = tokenResponse.accessToken;
        callUserInfo(accessToken);


    }


    private void callUserInfo(String accessToken) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                URL userInfoEndpoint = new URL("https://10.0.2.2:9443/oauth2/userinfo");
                HttpURLConnection conn = (HttpURLConnection) userInfoEndpoint.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setInstanceFollowRedirects(false);
                String response = Okio.buffer(Okio.source(conn.getInputStream())).readString(Charset.forName("UTF-8"));
                JSONObject json = new JSONObject(response);
                TextView username = (TextView) findViewById(R.id.username);

                TextView useremailId = (TextView) findViewById(R.id.emailid);

                username.setText(json.getString("sub"));
                useremailId.setText(json.getString("email"));

                TextView textViewName = findViewById(R.id.usernameview);
                TextView textViewEmail = findViewById(R.id.emailview);
                textViewEmail.setText("Email Address");
                textViewName.setText("Username");

                Button btnClick = (Button) findViewById(R.id.logout);
                btnClick.setOnClickListener(new UserInfoActivity.LogoutListener());

            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    public class LogoutListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            String logout_uri = "https://10.0.2.2:9443/oidc/logout";
            String redirect = "com.example.myapplication://oauth";
            StringBuffer url = new StringBuffer();
            url.append(logout_uri);
            url.append("?id_token_hint=");
            url.append(idToken);
            url.append("&post_logout_redirect_uri=");
            url.append(redirect);
            url.append("&state=");


            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            customTabsIntent.launchUrl(view.getContext(), Uri.parse(url.toString()));
        }
    }
}

