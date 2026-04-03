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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

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
                        
                         1. Log in to your Atlas CMMS account \s
                         2. Navigate to **Settings > Integrations > API Keys** \s
                         3. Click **Generate New Key** \s
                         4. Copy and securely store your key (it will only be shown once) \s
                         5. Use this key in the `x-api-key` header for all API requests \s
                        
                         ### Example Request
                        
                         ```bash
                         curl -X GET "https://api.atlas-cmms.com/locations" \\
                           -H "x-api-key: your_api_key_here"
                         ```
                        
                         ### API Key Best Practices
                        
                         - Keep your API key secure and never expose it publicly \s
                         - Store it in environment variables (e.g., `{{apiKey}}` in Postman) \s
                         - Rotate keys regularly \s
                         - Revoke compromised keys immediately \s
                        
                         ---
                        
                         ### Using Postman or Insomnia
                        
                         1. Download and install Postman or Insomnia \s
                         2. Import the OpenAPI specification from: `https://api.atlas-cmms.com/v3/api-docs/atlas-cmms` \s
                         3. Set up environment variables:
                            - `baseUrl`: Your API base URL (e.g., `https://api.atlas-cmms.com`) \s
                            - `apiKey`: Your API key \s
                        
                         ### Testing Your Setup
                        
                         To verify your configuration:
                        
                         1. Add the `x-api-key` header to your requests \s
                         2. Make a GET request to `/locations` \s
                         3. You should receive a JSON response with your organization's locations \s
                        
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
}
