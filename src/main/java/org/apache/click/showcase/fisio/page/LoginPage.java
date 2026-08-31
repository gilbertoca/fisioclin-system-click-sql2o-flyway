package org.apache.click.showcase.fisio.page;

import org.apache.click.control.Form;
import org.apache.click.control.PasswordField;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.util.Bindable;

/**
 * Página de Login com logout.
 */
public class LoginPage extends BorderPage {
    private static final long serialVersionUID = 1L;

    private Form form = new Form("form");

    /** Bindable para receber ?action=logout */
    @Bindable protected String action;

    public LoginPage() {
        addModel("title", "Login");
        addControl(form);

        form.setLayout(Form.LAYOUT_DIV);

        TextField username = new TextField("username", "Usuário", true);
        username.setMinLength(3);
        form.add(username);

        PasswordField password = new PasswordField("password", "Senha", true);
        form.add(password);

        form.add(new Submit("login", "Entrar", this, "onLoginClick"));
    }

    @Override
    public void onGet() {
        // Se veio com ?action=logout, desloga e redireciona
        if ("logout".equals(action)) {
            getContext().removeSessionAttribute("user");
            setRedirect(HomePage.class);
        }
    }

    public boolean onLoginClick() {
        if (form.isValid()) {
            String user = form.getFieldValue("username");
            String pass = form.getFieldValue("password");

            if ("admin".equals(user) && "admin".equals(pass)) {
                getContext().setSessionAttribute("user", user);
                setRedirect(HomePage.class);
                return false;
            } else {
                form.setError("Usuário ou senha inválidos");
            }
        }
        return true;
    }
}