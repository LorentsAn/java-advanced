package info.kgeorgiy.ja.lorents.bank;

import java.io.Serializable;

public class AccountImpl implements Account, Serializable {

    private final String id;
    private int amount;

    /**
     * Creates an account by id
     * @param id {@link String}
     */
    public AccountImpl(final String id) {
        this.id = id;
        amount = 0;
    }

    /**
     * Creates an account with a given amount of money/
     * @param id {@link String}
     * @param amount {@link int} amount of money
     */
    public AccountImpl(String id, int amount) {
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        return amount;
    }

    @Override
    public synchronized void setAmount(int amount) {
        this.amount = amount;
    }


}
