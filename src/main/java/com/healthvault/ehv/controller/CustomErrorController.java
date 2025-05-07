package com.healthvault.ehv.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);
    private final ErrorAttributes errorAttributes;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            // Log the error details
            logger.error("Error occurred: Status Code={}, Message={}, Exception={}", 
                    statusCode, message, exception);
            
            model.addAttribute("status", statusCode);
            model.addAttribute("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
            
            if (message != null && !message.toString().isEmpty()) {
                model.addAttribute("message", message);
            } else {
                model.addAttribute("message", "An unexpected error occurred. Please try again later.");
            }
            
            // Get trace if available
            WebRequest webRequest = new ServletWebRequest(request);
            Map<String, Object> errorAttributesMap = errorAttributes.getErrorAttributes(webRequest, 
                    org.springframework.boot.web.error.ErrorAttributeOptions.of(
                            org.springframework.boot.web.error.ErrorAttributeOptions.Include.STACK_TRACE,
                            org.springframework.boot.web.error.ErrorAttributeOptions.Include.MESSAGE,
                            org.springframework.boot.web.error.ErrorAttributeOptions.Include.BINDING_ERRORS));
            
            model.addAttribute("timestamp", errorAttributesMap.get("timestamp"));
            
            if (statusCode == 404) {
                return "error/404";
            } else if (statusCode == 403) {
                return "error/403";
            } else if (statusCode >= 500) {
                return "error";
            }
        }
        
        return "error";
    }
} 