package com.grash.model;

import com.grash.exception.CustomException;
import com.grash.model.abstracts.CompanyAudit;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import javax.persistence.Entity;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
public class Deprecation extends CompanyAudit {
    private long purchasePrice;

    private Date purchaseDate;

    private String residualValue;

    private String usefulLIfe;

    private int rate;

    private long currentValue;

    public void setRate(int rate){
        if(rate<0) throw new CustomException("The rate should not be negative", HttpStatus.NOT_ACCEPTABLE);
        this.rate=rate;
    }
}
