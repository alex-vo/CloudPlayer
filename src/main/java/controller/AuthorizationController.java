package controller;

import ejb.AuthorizationBeanRemote;
import model.LoginFormModel;
import model.RegisterFormModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import remote.RemotingManager;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

/**
 * Created with IntelliJ IDEA.
 * User: user
 * Date: 6/20/13
 * Time: 12:33 PM
 * To change this template use File | Settings | File Templates.
 */
@Controller
public class AuthorizationController {

    @Value("#{localProperties['dropbox.callback.url']}")
    public String DROPBOX_CALLBACK_URL;

    @Value("#{localProperties['drive.redirect.uri']}")
    public String DRIVE_REDIRECT_URI;

    @Value("#{localProperties['drive.client.id']}")
    public String DRIVE_CLIENT_ID;

    @Value("#{localProperties['jboss.login']}")
    public String JBOSS_LOGIN;

    @Value("#{localProperties['jboss.password']}")
    public String JBOSS_PASSWORD;

    @Value("#{localProperties['jboss.url']}")
    public String JBOSS_URL;

    @RequestMapping("/welcome")
    public String printWelcome(ModelMap model) {
        if(!model.containsAttribute("loginForm")) model.addAttribute("loginForm", new LoginFormModel());
        return "hello";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(@Valid @ModelAttribute("loginForm") LoginFormModel loginForm, BindingResult binding,
                        RedirectAttributes redirectAttributes, HttpSession httpSession){

        String result = "redirect:/welcome";
        if (binding.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.loginForm", binding);
            redirectAttributes.addFlashAttribute("loginForm", loginForm);
            redirectAttributes.addFlashAttribute("successMessage", "Failed to log in");
            return result;
        }

        RemotingManager remotingManager = null;
        try {
            remotingManager = new RemotingManager(JBOSS_URL, JBOSS_LOGIN, JBOSS_PASSWORD);
            Context context = remotingManager.getContext();
            AuthorizationBeanRemote bean = (AuthorizationBeanRemote) context
                    .lookup("ejb:/cp-core//AuthorizationBean!ejb.AuthorizationBeanRemote");
            Long userId = bean.login(loginForm.getLogin(), loginForm.getPassword());
            if(userId != null && userId > 0){
                httpSession.setAttribute("user", userId);
                result = "redirect:/app";
            }else{
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to log in");
            }
        } catch (NamingException ne) {
            redirectAttributes.addFlashAttribute("serverErrorMessage", "Failed to connect the server");
            ne.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(remotingManager != null){
                remotingManager.terminate();
            }
        }

        return result;
    }

    @RequestMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/welcome";
    }

    @RequestMapping("/addDropbox")
    public String addDropbox(RedirectAttributes redirectAttributes, HttpSession httpSession){
        String result = "redirect:app";
        RemotingManager remotingManager = null;
        try {
            //TODO make static
            remotingManager = new RemotingManager(JBOSS_URL, JBOSS_LOGIN, JBOSS_PASSWORD);
            Context context = remotingManager.getContext();
            AuthorizationBeanRemote bean = (AuthorizationBeanRemote) context
                    .lookup("ejb:/cp-core//AuthorizationBean!ejb.AuthorizationBeanRemote");
            String dropboxUrl = bean.getDropboxAuthLink((Long) httpSession.getAttribute("user"));
            if(dropboxUrl != null){
                result = "redirect:" + dropboxUrl + "&oauth_callback=" + DROPBOX_CALLBACK_URL; //TODO move this to core
            }
        } catch (NamingException ne) {
            redirectAttributes.addFlashAttribute("serverErrorMessage", "Failed to connect the server");
            ne.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(remotingManager != null){
                remotingManager.terminate();
            }
        }
        return result;
    }

    @RequestMapping("/removeDropbox")
    public String removeDropbox(RedirectAttributes redirectAttributes, HttpSession httpSession){

        RemotingManager remotingManager = null;
        try {
            remotingManager = new RemotingManager(JBOSS_URL, JBOSS_LOGIN, JBOSS_PASSWORD);
            Context context = remotingManager.getContext();
            AuthorizationBeanRemote bean = (AuthorizationBeanRemote) context
                    .lookup("ejb:/cp-core//AuthorizationBean!ejb.AuthorizationBeanRemote");
            Boolean removedDropboxAccount = bean.removeDropboxAcoount((Long) httpSession.getAttribute("user"));
            if(removedDropboxAccount){
                redirectAttributes.addFlashAttribute("successMessage", "Removed Dropbox account");
            }
        } catch (NamingException e) {
            redirectAttributes.addFlashAttribute("serverErrorMessage", "Failed to connect the server");
            e.printStackTrace();
        } finally {
            if(remotingManager != null){
                remotingManager.terminate();
            }
        }
        return "redirect:app";
    }

    @RequestMapping("dropboxAuthComplete")
    public String dropboxAuthComplete(RedirectAttributes redirectAttributes, HttpSession httpSession){

        RemotingManager remotingManager = null;
        try {
            //TODO make static
            remotingManager = new RemotingManager(JBOSS_URL, JBOSS_LOGIN, JBOSS_PASSWORD);
            Context context = remotingManager.getContext();
            AuthorizationBeanRemote bean = (AuthorizationBeanRemote) context
                    .lookup("ejb:/cp-core//AuthorizationBean!ejb.AuthorizationBeanRemote");
            Boolean retrievedToken = bean.retrieveDropboxAccessToken((Long) httpSession.getAttribute("user"));
            if(retrievedToken){
                redirectAttributes.addFlashAttribute("successMessage", "Added Dropbox account");
            }else{
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to add Dropbox account");
            }
        } catch (NamingException ne) {
            redirectAttributes.addFlashAttribute("serverErrorMessage", "Failed to connect the server");
            ne.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(remotingManager != null){
                remotingManager.terminate();
            }
        }
        return "redirect:app";
    }

    @RequestMapping("/addDrive")
    public String addGDrive(HttpSession httpSession){
        return "redirect:https://accounts.google.com/o/oauth2/auth?redirect_uri="
                + DRIVE_REDIRECT_URI
                + "&response_type=code&client_id=" + DRIVE_CLIENT_ID
                + "&scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fdrive"
                + "+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email&approval_prompt=force&access_type=offline";
    }

    @RequestMapping("/removeDrive")
    public String removeDrive(RedirectAttributes redirectAttributes, HttpSession httpSession){

        RemotingManager remotingManager = null;
        try {
            remotingManager = new RemotingManager(JBOSS_URL, JBOSS_LOGIN, JBOSS_PASSWORD);
            Context context = remotingManager.getContext();
            AuthorizationBeanRemote bean = (AuthorizationBeanRemote) context
                    .lookup("ejb:/cp-core//AuthorizationBean!ejb.AuthorizationBeanRemote");
            Boolean removedDriveAccount = bean.removeGDriveAccount((Long) httpSession.getAttribute("user"));
            if(removedDriveAccount){
                redirectAttributes.addFlashAttribute("successMessage", "Removed Google Drive account");
            }
        } catch (NamingException e) {
            redirectAttributes.addFlashAttribute("serverErrorMessage", "Failed to connect the server");
            e.printStackTrace();
        } finally {
            if(remotingManager != null){
                remotingManager.terminate();
            }
        }
        return "redirect:app";
    }

    @RequestMapping("driveAuthComplete")
    public String driveAuthComplete(@RequestParam(value = "code") String code,
                                    RedirectAttributes redirectAttributes,
                                    HttpSession httpSession){
        RemotingManager remotingManager = null;
        try {
            remotingManager = new RemotingManager(JBOSS_URL, JBOSS_LOGIN, JBOSS_PASSWORD);
            Context context = remotingManager.getContext();
            AuthorizationBeanRemote bean = (AuthorizationBeanRemote) context
                    .lookup("ejb:/cp-core//AuthorizationBean!ejb.AuthorizationBeanRemote");
            Boolean retrievedCredentials = bean.retrieveGDriveCredentials((Long) httpSession.getAttribute("user"), code);
            if(retrievedCredentials){
                redirectAttributes.addFlashAttribute("successMessage", "Added Google Drive account");
            }else{
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to add Google Drive account");
            }
        } catch (NamingException e) {
            redirectAttributes.addFlashAttribute("serverErrorMessage", "Failed to connect the server");
            e.printStackTrace();
        } finally {
            if(remotingManager != null){
                remotingManager.terminate();
            }
        }
        return "redirect:app";
    }

    @RequestMapping("/registerForm")
    public String registerForm(ModelMap model){
        if(!model.containsAttribute("registerForm")) model.addAttribute("registerForm", new RegisterFormModel());
        return "registerForm";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String register(@Valid @ModelAttribute("registerForm") RegisterFormModel registerFormModel,
                           BindingResult binding,
                           RedirectAttributes redirectAttributes){
        if(!registerFormModel.getPassword().equals(registerFormModel.getRepeatPassword())){
            binding.addError(new FieldError("loginForm", "password", registerFormModel.getPassword(), true, null, null, "Passwords must match"));
        }
        if(binding.hasErrors()){
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerForm", binding);
            redirectAttributes.addFlashAttribute("registerForm", registerFormModel);
            return "redirect:registerForm";
        }

        RemotingManager remotingManager = null;
        try {
            //TODO make static
            remotingManager = new RemotingManager(JBOSS_URL, JBOSS_LOGIN, JBOSS_PASSWORD);
            Context context = remotingManager.getContext();
            AuthorizationBeanRemote bean = (AuthorizationBeanRemote) context
                    .lookup("ejb:/cp-core//AuthorizationBean!ejb.AuthorizationBeanRemote");
            Boolean registered = bean.registerUser(registerFormModel.getLogin(), registerFormModel.getPassword());
            if(registered){
                redirectAttributes.addFlashAttribute("successMessage", "Registration completed successfully");
            }else{
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to register");
            }
        } catch (NamingException ne) {
            redirectAttributes.addFlashAttribute("serverErrorMessage", "Failed to connect the server");
            ne.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(remotingManager != null){
                remotingManager.terminate();
            }
        }
        return "redirect:welcome";
    }

    @RequestMapping("/signInDrive")
    public String signInDrive(){
        return "redirect:app";
    }
}
