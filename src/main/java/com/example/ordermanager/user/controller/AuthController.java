package com.example.ordermanager.user.controller;

import com.example.ordermanager.exception.EmailAlreadySentException;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.RegistrationService;
import com.example.ordermanager.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final RegistrationService registrationService;

    public AuthController(UserService userService, RegistrationService registrationService) {
        this.userService = userService;
        this.registrationService = registrationService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("user", new User());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String registerUser(@ModelAttribute User user, Model model) {
        if (userService.findByUsername(user.getUsername()) != null) {
            model.addAttribute("error", "Username already exists!");
            return "auth/signup";
        }
        userService.register(user);
        model.addAttribute("message", "Verification email sent! Please check your inbox.");
        return "auth/login";
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        try {
            boolean verified = registrationService.verifyToken(token);

            if (verified) {
                redirectAttributes.addFlashAttribute("message",
                        "Your email has been verified successfully! You can now log in.");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Invalid or expired verification link. Please request a new one.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Something went wrong during verification. Please try again later.");
        }

        return "redirect:/login";
    }


    @GetMapping("/resend-verification")
    public String resendVerificationPage(@RequestParam(value = "email", required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "auth/resend-verification";
    }


    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam("email") String input, RedirectAttributes redirectAttributes) {
        boolean isEmail = input.contains("@");

        User user = isEmail ? userService.findByEmail(input)
                : userService.findByUsername(input);

        if (user == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Please enter a valid " + (isEmail ? "email address." : "username."));
            return "redirect:/resend-verification";
        }

        if (user.isEnabled()) {
            redirectAttributes.addFlashAttribute("message",
                    "Your account is already verified. You can log in now.");
            return "redirect:/login";
        }

        try {
            registrationService.sendVerificationEmail(user);
            redirectAttributes.addFlashAttribute("message",
                    "Verification email resent successfully! Please check your inbox.");
        } catch (EmailAlreadySentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Something went wrong while sending the verification email. Please try again later.");
        }

        return "redirect:/login";
    }





}
