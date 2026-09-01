package com.pawmate.backend.mail;

import com.pawmate.backend.config.AppProperties;
import com.pawmate.backend.entity.User;
import com.pawmate.backend.exception.EmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envoi des emails transactionnels PawMate. Les liens de vérification /
 * réinitialisation ne sont jamais hardcodés : ils sont construits à partir de
 * la configuration (lien web ou deep-link mobile selon le schéma configuré).
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    public EmailService(JavaMailSender mailSender, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.appProperties = appProperties;
    }

    public void sendVerificationEmail(User user, String token, String code, long expirationMinutes) {
        String link = buildVerificationLink(token);

        String html = buildVerificationHtml(link, code, expirationMinutes);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(appProperties.getMail().getFrom());
            helper.setTo(user.getEmail());
            helper.setSubject("PawMate — Vérifiez votre adresse email");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new EmailDeliveryException("Impossible d'envoyer l'email de vérification");
        }
    }

    public void sendPasswordResetEmail(User user, String token, long expirationMinutes) {
        String link = buildResetLink(token);

        String html = buildResetHtml(link, expirationMinutes);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(appProperties.getMail().getFrom());
            helper.setTo(user.getEmail());
            helper.setSubject("PawMate — Réinitialisation de mot de passe");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new EmailDeliveryException("Impossible d'envoyer l'email de réinitialisation");
        }
    }

    /**
     * Construit le lien de vérification. Si un schéma de deep-link mobile est
     * configuré (ex. "pawmate"), on génère un lien custom que l'app sait ouvrir
     * (pawmate://verify), sinon on retombe sur le lien web classique.
     */
    private String buildVerificationLink(String token) {
        String scheme = appProperties.getVerification().getDeepLinkScheme();
        if (scheme != null && !scheme.isBlank()) {
            return scheme + "://" + appProperties.getVerification().getDeepLinkHost() + "?token=" + token;
        }
        return appProperties.getVerification().getBaseUrl() + "/auth/verify-email?token=" + token;
    }

    /**
     * Construit le lien de réinitialisation de mot de passe (même logique de
     * deep-link mobile que pour la vérification d'email).
     */
    private String buildResetLink(String token) {
        String scheme = appProperties.getVerification().getDeepLinkScheme();
        if (scheme != null && !scheme.isBlank()) {
            return scheme + "://" + appProperties.getVerification().getDeepLinkHost() + "/reset?token=" + token;
        }
        return appProperties.getVerification().getBaseUrl() + "/auth/reset-password?token=" + token;
    }

    private String buildVerificationHtml(String link, String code, long expirationMinutes) {
        return VERIFICATION_HTML_TEMPLATE
                .replace("__VERIFICATION_URL__", link)
                .replace("__VERIFICATION_CODE__", code)
                .replace("__EXPIRATION_MINUTES__", String.valueOf(expirationMinutes));
    }

    private String buildResetHtml(String link, long expirationMinutes) {
        return RESET_HTML_TEMPLATE
                .replace("__RESET_URL__", link)
                .replace("__EXPIRATION_MINUTES__", String.valueOf(expirationMinutes));
    }

    private static final String VERIFICATION_HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="fr" xmlns="http://www.w3.org/1999/xhtml">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <meta http-equiv="X-UA-Compatible" content="IE=edge">
              <title>PawMate — Vérification email</title>
            </head>
            <body style="margin:0; padding:0; background-color:#FFF9F3;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0"
                     style="background-color:#FFF9F3; padding:32px 16px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0"
                           style="max-width:600px; background-color:#FFFFFF; border-radius:14px; overflow:hidden;
                                  border:1px solid #F4B860;">
                      <tr>
                        <td align="center" style="padding:36px 36px 20px 36px;">
                          <div style="font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:28px; font-weight:800;
                                      color:#E9785B; letter-spacing:1px;">PawMate</div>
                          <div style="font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:12px; font-weight:600;
                                      color:#8FAF9A; letter-spacing:4px; margin-top:4px;">TROUVEZ LE COMPAGNON IDÉAL</div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:8px 36px;">
                          <div style="height:4px; background-color:#F4B860; border-radius:2px;"></div>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:28px 36px 8px 36px;">
                          <h1 style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:24px;
                                     font-weight:700; color:#243447;">Bienvenue sur PawMate !</h1>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:16px 36px 8px 36px;">
                          <p style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:16px;
                                    line-height:1.6; color:#243447;">
                            Votre compte a été créé avec succès.
                          </p>
                          <p style="margin:12px 0 0 0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:15px;
                                    line-height:1.6; color:#243447;">
                            Pour commencer à utiliser PawMate, veuillez confirmer votre adresse email.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:24px 36px 12px 36px;">
                          <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                              <td align="center" style="border-radius:8px; background-color:#E9785B;">
                                <a href="__VERIFICATION_URL__"
                                   style="display:inline-block; padding:14px 32px; font-family:'Segoe UI',Helvetica,Arial,sans-serif;
                                          font-size:15px; font-weight:700; color:#FFFFFF; text-decoration:none;
                                          border-radius:8px; line-height:1.4;">
                                  Vérifier mon adresse email
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:20px 36px 0 36px;">
                          <p style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:14px;
                                    color:#243447;">
                            … ou saisissez ce code dans l'application :
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:14px 36px 0 36px;">
                          <table role="presentation" cellpadding="0" cellspacing="0" border="0"
                                 style="border-collapse:collapse; background-color:#FFF9F3; border:2px dashed #E9785B;
                                        border-radius:10px;">
                            <tr>
                              <td align="center" style="padding:18px 40px;
                                                         font-family:Consolas,'Courier New',monospace; font-size:32px;
                                                         font-weight:700; letter-spacing:8px; color:#243447;">
                                __VERIFICATION_CODE__
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:10px 36px 8px 36px;">
                          <p style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:12px;
                                    color:#8FAF9A;">
                            Code à 6 chiffres, à saisir dans l'application.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:8px 36px 8px 36px;">
                          <p style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:12px;
                                    color:#8FAF9A;">
                            Ce lien et ce code sont valables pendant __EXPIRATION_MINUTES__ minutes.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:0 36px 24px 36px;">
                          <p style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:13px;
                                    line-height:1.5; color:#8FAF9A;">
                            Si vous n'êtes pas à l'origine de cette inscription, ignorez simplement cet email.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="background-color:#243447; padding:20px 36px;">
                          <p style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:13px;
                                    color:#8FAF9A; text-align:center;">
                            PawMate &copy; — Des animaux heureux, des humains comblés.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

    private static final String RESET_HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="fr" xmlns="http://www.w3.org/1999/xhtml">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <meta http-equiv="X-UA-Compatible" content="IE=edge">
              <title>PawMate — Réinitialisation de mot de passe</title>
            </head>
            <body style="margin:0; padding:0; background-color:#FFF9F3;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0"
                     style="background-color:#FFF9F3; padding:32px 16px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0"
                           style="max-width:600px; background-color:#FFFFFF; border-radius:14px; overflow:hidden;
                                  border:1px solid #F4B860;">
                      <tr>
                        <td align="center" style="padding:36px 36px 20px 36px;">
                          <div style="font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:28px; font-weight:800;
                                      color:#E9785B; letter-spacing:1px;">PawMate</div>
                          <div style="font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:12px; font-weight:600;
                                      color:#8FAF9A; letter-spacing:4px; margin-top:4px;">TROUVEZ LE COMPAGNON IDÉAL</div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:8px 36px;">
                          <div style="height:4px; background-color:#F4B860; border-radius:2px;"></div>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:28px 36px 8px 36px;">
                          <h1 style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:24px;
                                     font-weight:700; color:#243447;">Réinitialisez votre mot de passe</h1>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:16px 36px 8px 36px;">
                          <p style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:16px;
                                    line-height:1.6; color:#243447;">
                            Vous avez demandé à réinitialiser votre mot de passe PawMate.
                          </p>
                          <p style="margin:12px 0 0 0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:15px;
                                    line-height:1.6; color:#243447;">
                            Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe. Ce lien
                            est valable pendant __EXPIRATION_MINUTES__ minutes.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:24px 36px 12px 36px;">
                          <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                            <tr>
                              <td align="center" style="border-radius:8px; background-color:#E9785B;">
                                <a href="__RESET_URL__"
                                   style="display:inline-block; padding:14px 32px; font-family:'Segoe UI',Helvetica,Arial,sans-serif;
                                          font-size:15px; font-weight:700; color:#FFFFFF; text-decoration:none;
                                          border-radius:8px; line-height:1.4;">
                                  Réinitialiser mon mot de passe
                                </a>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:0 36px 24px 36px;">
                          <p style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:13px;
                                    line-height:1.5; color:#8FAF9A;">
                            Si vous n'êtes pas à l'origine de cette demande, ignorez cet email : votre mot de
                            passe actuel reste inchangé.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="background-color:#243447; padding:20px 36px;">
                          <p style="margin:0; font-family:'Segoe UI',Helvetica,Arial,sans-serif; font-size:13px;
                                    color:#8FAF9A; text-align:center;">
                            PawMate &copy; — Des animaux heureux, des humains comblés.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;
}