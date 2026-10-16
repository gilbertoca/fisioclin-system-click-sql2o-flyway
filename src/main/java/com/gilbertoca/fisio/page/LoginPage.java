package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.service.AuthService;
import org.apache.click.control.Form;
import org.apache.click.control.PasswordField;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;

/**
 * Login page.
 */
public class LoginPage extends BorderPage {

    private final AuthService authService = new AuthService();

    private Form form = new Form("form");
    private TextField username = new TextField("username", "Username", true);
    private PasswordField password = new PasswordField("password", "Password", true);

    public LoginPage() {
        form.setNoLayout(true);
        form.add(username);
        form.add(password);
        form.add(new Submit("login", "Sign in", this, "onLogin"));
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Sign in · Click + Bulma Demo");
    }

    public boolean onLogin() {
        if (form.isValid()) {
            if (authService.authenticate(username.getValue(), password.getValue())) {
                getContext().setSessionAttribute(AuthenticatedPage.SESSION_USER, username.getValue());
                setRedirect(HomePage.class);
                return false;
            } else {
                form.setError("Invalid username or password");
            }
        }
        return true;
    }
}
