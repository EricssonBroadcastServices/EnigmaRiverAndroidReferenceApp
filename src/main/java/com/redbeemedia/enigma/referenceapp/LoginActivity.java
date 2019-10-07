package com.redbeemedia.enigma.referenceapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.redbeemedia.enigma.core.error.CredentialsError;
import com.redbeemedia.enigma.core.error.DeviceLimitReachedError;
import com.redbeemedia.enigma.core.error.EnigmaError;
import com.redbeemedia.enigma.core.error.HttpResourceNotFoundError;
import com.redbeemedia.enigma.core.error.InvalidCredentialsError;
import com.redbeemedia.enigma.core.error.LoginDeniedError;
import com.redbeemedia.enigma.core.error.SessionLimitExceededError;
import com.redbeemedia.enigma.core.error.UnknownBusinessUnitError;
import com.redbeemedia.enigma.core.login.EnigmaLogin;
import com.redbeemedia.enigma.core.login.ILoginResultHandler;
import com.redbeemedia.enigma.core.login.UserLoginRequest;
import com.redbeemedia.enigma.core.session.ISession;
import com.redbeemedia.enigma.referenceapp.session.SessionContainer;

import java.lang.ref.WeakReference;

public class LoginActivity extends AppCompatActivity {
    private Handler handler;
    private LoginResultHandler loginResultHandler = new LoginResultHandler(this);
    private EnigmaLogin enigmaLogin;
    private boolean waitingForLogin = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SessionContainer.setSession(null);

        this.handler = new Handler();
        this.enigmaLogin = new EnigmaLogin(EnigmaRiverReferenceApp.BUSINESS_UNIT);
        this.enigmaLogin.setCallbackHandler(handler);

        Button button = findViewById(R.id.button_login);
        EditText usernameField = findViewById(R.id.edit_login_username);
        EditText passwordField = findViewById(R.id.edit_login_password);

        button.setOnClickListener(view -> {
            if(!waitingForLogin) {
                UserLoginRequest userLoginRequest = new UserLoginRequest(usernameField.getText().toString(), passwordField.getText().toString(), loginResultHandler);
                waitingForLogin = true;
                enigmaLogin.login(userLoginRequest);
            }
        });
    }

    private void onLoginSuccess(ISession session) {
        SessionContainer.setSession(session);
        if(waitingForLogin) {
            startActivity(new Intent(this, ListAssetsActivity.class));
            waitingForLogin = false;
        }
    }

    private void onLoginError(EnigmaError error) {
        waitingForLogin = false;
        String errorMessage = getErrorForUser(error);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        error.printStackTrace();
    }

    private String getErrorForUser(EnigmaError error) {
        if(error instanceof InvalidCredentialsError) {
            return getString(R.string.error_invalid_credentials);
        } else if(error instanceof SessionLimitExceededError) {
            return getString(R.string.error_session_limit_exceeded);
        } else if(error instanceof DeviceLimitReachedError) {
            return getString(R.string.error_device_limit_exceeded);
        } else if(error instanceof HttpResourceNotFoundError) {
            return getString(R.string.error_incorrect_configuration);
        } else if(error instanceof UnknownBusinessUnitError) {
            return getString(R.string.error_incorrect_configuration);
        } else if(error instanceof LoginDeniedError) {
            return getString(R.string.error_login_denied);
        } else if(error instanceof CredentialsError) {
            return getString(R.string.error_credentials);
        } else {
            return getString(R.string.error_unknown);
        }
    }

    private static class LoginResultHandler implements ILoginResultHandler {
        private WeakReference<LoginActivity> activityReference;
        public LoginResultHandler(LoginActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(ISession session) {
            LoginActivity activity = activityReference.get();
            if(activity != null) {
                activity.onLoginSuccess(session);
            }
        }

        @Override
        public void onError(EnigmaError error) {
            LoginActivity activity = activityReference.get();
            if(activity != null) {
                activity.onLoginError(error);
            }
        }
    }
}
