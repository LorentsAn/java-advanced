package info.kgeorgiy.ja.lorents.bank;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class LocalPerson extends AbstractPerson implements Serializable {

    private final Map<String, Account> accounts;

    public LocalPerson(String firstName, String lastName, String passportNumber, Map<String, Account> accounts) {
        super(firstName, lastName, passportNumber);
        this.accounts = accounts;
    }

    public Set<String> getAccounts() {
        return accounts.keySet();
    }

    public Account findAccountById(String id) {
        return accounts.get(id);
    }

    public void putAccount(String id, AccountImpl account) {
        accounts.put(id, account);
    }

}
