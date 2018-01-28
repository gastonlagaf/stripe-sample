package com.gastonlagaf.stripe;

import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.*;

import java.io.Console;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StripeService {

    private static final Console console = System.console();

    private static Charge createCharge(Source source) throws CardException, APIException, AuthenticationException, InvalidRequestException, APIConnectionException {
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", 2000);
        chargeParams.put("currency", "usd");
        chargeParams.put("description", "Charge for description");
        chargeParams.put("source", source.getId());
        return Charge.create(chargeParams);
    }

    private static Charge createCharge(Token token) throws CardException, APIException, AuthenticationException, InvalidRequestException, APIConnectionException {
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", 2000);
        chargeParams.put("currency", "usd");
        chargeParams.put("description", "Charge for description");
        chargeParams.put("source", token.getId());
        return Charge.create(chargeParams);
    }

    private static Source createCardSource(String number, Integer expMonth, Integer expYear, String cvc) throws CardException, APIException, AuthenticationException, InvalidRequestException, APIConnectionException {
        Map<String, Object> sourceParams = new HashMap<>();
        sourceParams.put("type", "card");
        sourceParams.put("currency", "usd");
        Map<String, Object> cardParams = new HashMap<>();
        cardParams.put("number", number);
        cardParams.put("exp_month", expMonth);
        cardParams.put("exp_year", expYear);
        cardParams.put("cvc", cvc);
        sourceParams.put("card", cardParams);
        return Source.create(sourceParams);
    }

    private static Token createBankToken(String currency, String routingNumber, String accountNumber, String holderName, String holderType) throws CardException, APIException, AuthenticationException, InvalidRequestException, APIConnectionException {
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> token = new HashMap<>();
        params.put("currency", currency);
        params.put("routing_number", routingNumber);
        params.put("account_number", accountNumber);
        params.put("account_holder_name", holderName);
        params.put("account_holder_type", holderType);
        params.put("country", "us");
        token.put("bank_account", params);
        Token result = Token.create(token);
        Map<String, Object> customerParams = new HashMap<>();
        customerParams.put("email", "sampleemail@test.com");
        customerParams.put("source", result.getId());
        Customer customer = Customer.create(customerParams);
        console.printf("Please, verify your bank account\n");
        Map<String, Object> verificationParams = new HashMap<>();
        List<Integer> amounts = new ArrayList<>(2);
        amounts.add(new Integer(console.readLine("Insert first amount: ")));
        amounts.add(new Integer(console.readLine("Insert second amount: ")));
        verificationParams.put("amounts", amounts);
        customer.getSources().retrieve(result.getBankAccount().getId()).verify(verificationParams);
        return result;
    }

    private static Source create3ds(Source source) throws CardException, APIException, AuthenticationException, InvalidRequestException, APIConnectionException {
        Map<String, Object> sourceParams = new HashMap<>();
        sourceParams.put("amount", 2000);
        sourceParams.put("currency", "usd");
        sourceParams.put("type", "three_d_secure");
        Map<String, Object> redirectParams = new HashMap<>();
        redirectParams.put("return_url", "https://shop.example.com/dashboard");
        sourceParams.put("redirect", redirectParams);
        Map<String, Object> threeDSecureParams = new HashMap<>();
        threeDSecureParams.put("card", source.getId());
        sourceParams.put("three_d_secure", threeDSecureParams);
        return Source.create(sourceParams);
    }

    public static void main(String... args) throws CardException, APIException, AuthenticationException, InvalidRequestException, APIConnectionException {
        Stripe.apiKey = "";
        Charge charge;
        Map<String, String> sourceParams = askSourceParameters();
        if(sourceParams.containsKey("cardNumber")) {
            Source source = createCardSource(sourceParams.get("cardNumber"), new Integer(sourceParams.get("expMonth")),
                    new Integer(sourceParams.get("expYear")), sourceParams.get("cvc"));
            if("required".equals(source.getTypeData().get("three_d_secure"))) {
                source = create3ds(source);
                console.readLine("Here is your 3ds redirect url (" + source.getRedirect().getURL() + "). Verify payment and proceed charge");
            }
            charge = createCharge(source);
        } else {
            Token bankToken = createBankToken(sourceParams.get("currency"), sourceParams.get("routingNumber"),
                    sourceParams.get("accountNumber"), sourceParams.get("account_holder_name"), sourceParams.get("account_holder_type"));
            charge = createCharge(bankToken);
        }
        console.printf("Charge is ended up. The status is " + charge.getStatus() + "\n");
    }

    private static Map<String, String> askSourceParameters() {
        String sourceAnsw;
        Map<String, String> result = new HashMap<>();
        //do {
            sourceAnsw = console.readLine("How you want to pay (bank account (insert b) / card (insert c)): ");
            switch (sourceAnsw) {
                case "b":
                    result.put("currency", console.readLine("Insert currency: "));
                    result.put("routingNumber", console.readLine("Insert routing number: "));
                    result.put("accountNumber", console.readLine("Insert account number: "));
                    result.put("account_holder_name", console.readLine("Insert holder name: "));
                    result.put("account_holder_type", console.readLine("Insert holder type: "));
                    break;
                case "c":
                    result.put("cardNumber", console.readLine("Insert card number: "));
                    result.put("expMonth", console.readLine("Insert expiration month: "));
                    result.put("expYear", console.readLine("Insert expiration year: "));
                    result.put("cvc", console.readLine("Insert cvc: "));
                    break;
                default:
                    console.writer().write("Wrong payment option");
            }
        //} while (!"b".equals(sourceAnsw) || !"c".equals(sourceAnsw));
        return result;
    }

}
