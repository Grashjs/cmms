package com.grash.model;

import com.grash.exception.CustomException;
import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import jakarta.persistence.Entity;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Depreciation entity for tracking asset value over time")
public class Deprecation extends CompanyAudit {
    @Schema(description = "Original purchase price")
    private long purchasePrice;

    @Schema(description = "Date of purchase")
    private Date purchaseDate;

    @Schema(description = "Residual value after depreciation")
    private String residualValue;

    @Schema(description = "Useful life of the asset")
    private String usefulLIfe;

    @Schema(description = "Depreciation rate (percentage)")
    private int rate;

    @Schema(description = "Current value after depreciation")
    private long currentValue;

    public void setRate(int rate){
        if(rate<0) throw new CustomException("The rate should not be negative", HttpStatus.NOT_ACCEPTABLE);
        this.rate=rate;
    }
}

