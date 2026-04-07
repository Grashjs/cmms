package com.grash.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Atlas CMMS API",
                version = "v1",
//                language=Md
                description = """
                         ## Getting Started

                         Welcome to the **Atlas CMMS API** documentation. This RESTful API provides programmatic access to all features of the Atlas Computerized Maintenance Management System (CMMS).

                         ### Base URL

                         All API requests should be made to:

                         ```
                         https://api.atlas-cmms.com
                         ```

                         ---

                         ## Authentication

                         All API endpoints require authentication using an **API Key**.

                         ### API Key Authentication

                         Include your API key in the request header:

                         ```
                         x-api-key: {your_api_key}
                         ```

                         ### Obtaining an API Key

                         1. Log in to your Atlas CMMS account 
                         2. Navigate to **Settings > Integrations > API Keys** 
                         3. Click **Generate New Key** 
                         4. Copy and securely store your key (it will only be shown once) 
                         5. Use this key in the `x-api-key` header for all API requests 

                         ### Example Request

                         ```bash
                         curl -X GET "https://api.atlas-cmms.com/locations" \
                           -H "x-api-key: your_api_key_here"
                         ```

                         ### API Key Best Practices

                         - Keep your API key secure and never expose it publicly 
                         - Store it in environment variables (e.g., `{{apiKey}}` in Postman) 
                         - Rotate keys regularly 
                         - Revoke compromised keys immediately 

                         ---

                         ### Using Postman or Insomnia

                         1. Download and install Postman or Insomnia 
                         2. Import the OpenAPI specification from: `https://api.atlas-cmms.com/v3/api-docs/atlas-cmms` 
                         3. Set up environment variables:
                            - `baseUrl`: Your API base URL (e.g., `https://api.atlas-cmms.com`) 
                            - `apiKey`: Your API key 

                         ### Testing Your Setup

                         To verify your configuration:

                         1. Add the `x-api-key` header to your requests 
                         2. Make a GET request to `/locations` 
                         3. You should receive a JSON response with your organization's locations 

                         ---

                         ### Rate Limiting

                         The Atlas CMMS API implements rate limiting to ensure fair usage and system stability.

                        """,
                contact = @Contact(
                        name = "Atlas CMMS Support",
                        email = "contact@atlas-cmms.com"
                ),
                license = @License(
                        name = "Proprietary"
                )
        ),
        security = {
                @SecurityRequirement(name = "apiKey")
        },
        servers = {
                @Server(url = "https://api.atlas-cmms.com", description = "Production server"),
                @Server(url = "http://localhost:8080", description = "Development server")
        }
)
@SecurityScheme(
        name = "apiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "x-api-key",
        description = "Enter your API key. You can generate one from Settings > Integrations > API Keys in your Atlas" +
                " CMMS account."
)
public class OpenApiConfig {
    // Configuration is handled via annotations

    @Bean
    public GroupedOpenApi webhookApi() {
        return GroupedOpenApi.builder()
                .group("Subscriptions & Webhooks")
                .packagesToScan("com.grash.controller")
                .pathsToMatch("/webhook-endpoints/**", "/webhook-docs/**")
                .addOpenApiCustomizer(openApi -> openApi.info(
                        new io.swagger.v3.oas.models.info.Info()
                                .title("Atlas CMMS Webhooks API")
                                .version("v1")
                                .description("""
                                        ## Webhooks in Atlas CMMS
                                        
                                        Webhooks are HTTP callbacks that allow different systems to communicate with each other in real-time.
                                        They're like automated messengers that deliver information when something happens, rather than requiring
                                        you to ask for it.
                                        
                                        In the context of Atlas CMMS, webhooks are a way for our system to automatically notify your application
                                        when specific events occur in your account. Instead of your application repeatedly checking our API for
                                        updates (a process known as "polling"), webhooks allow you to receive real-time notifications about important
                                        events like:
                                        
                                        - Work order status changes
                                        - New work orders being created
                                        - Asset status changes
                                        - Part quantity changes
                                        - And more...
                                        
                                        When an event occurs, Atlas CMMS sends an HTTP POST request to the endpoint you specify.
                                        The request contains details about the event. It allows your application to react immediately
                                        to changes in Atlas CMMS.
                                        
                                        ## Quick Links
                                        
                                        - `/webhook-docs/guide` - Comprehensive webhook guide
                                        - `/webhook-docs/events` - All available event types with examples
                                        - `/webhook-docs/security/example` - Security verification code examples
                                        """)
                ))
                .build();
    }
}
