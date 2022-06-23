package info.kgeorgiy.ja.lorents.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class RemoteBank implements Bank {

    private final int port;

    private final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> accountsByPassport = new ConcurrentHashMap<>();


    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public boolean createAccount(final String subId, Person person) throws RemoteException {
        if (subId == null || person == null) {
            return false;
        }

        String accountId = person.getPassportNumber() + ":" + subId;

        if (accounts.containsKey(accountId)) {
            return false;
        }
        Account account = new AccountImpl(subId);
        accounts.put(accountId, account);
        try {
            UnicastRemoteObject.exportObject(account, port);
        } catch (RemoteException e) {
            throw new RemoteException("Failed to export account");
        }
        if (accountsByPassport.get(person.getPassportNumber()) == null) {
            accountsByPassport.put(person.getPassportNumber(), new ConcurrentSkipListSet<>());
        }

        accountsByPassport.get(person.getPassportNumber()).add(subId);

        if (person instanceof LocalPerson) {
            ((LocalPerson) person).putAccount(subId, new AccountImpl(subId));
        }
        return true;
    }

    @Override
    public boolean createPerson(String firstName, String LastName, String passportNumber) throws RemoteException {
        if (firstName == null || LastName == null || passportNumber == null || persons.get(passportNumber) != null) {
            return false;
        }

        RemotePerson person = new RemotePerson(firstName, LastName, passportNumber);
        persons.put(passportNumber, person);
        accountsByPassport.put(passportNumber, new ConcurrentSkipListSet<>());
        try {
            UnicastRemoteObject.exportObject(person, port);
        } catch (RemoteException e) {
            throw new RemoteException("Failed to export person");
        }
        return true;
    }

    @Override
    public Account getAccount(final String subId, Person person) throws RemoteException {
        if (subId == null || person == null) {
            return null;
        }

        String accountId = person.getPassportNumber() + ":" + subId;

        if (accounts.containsKey(accountId)) {
            if (person instanceof LocalPerson) {
                return ((LocalPerson) person).findAccountById(subId);
            } else {
                return accounts.get(accountId);
            }
        }
        return null;
    }

    @Override
    public Set<String> getAccountsOfPerson(Person person) throws RemoteException {
        if (person == null) {
            return null;
        }
        if (person instanceof LocalPerson) {
            return ((LocalPerson) person).getAccounts();
        }
        return accountsByPassport.get(person.getPassportNumber());
    }

    @Override
    public Person getLocalPerson(String passportNumber) throws RemoteException {

        if (passportNumber == null || !persons.containsKey(passportNumber)) {
            return null;
        }

        Person person = persons.get(passportNumber);
        Map<String, Account> localAccount = new ConcurrentHashMap<>();

        getAccountsOfPerson(person).forEach((x) -> {
            try {
                Account currentAccount = getAccount(x, person);
                localAccount.put(x, new AccountImpl(currentAccount.getId(), currentAccount.getAmount()));
            } catch (RemoteException e) {
                System.err.println("Unable to get a local person" + e.getMessage());
            }
        });
        return new LocalPerson(person.getFirstName(), person.getLastName(), person.getPassportNumber(), localAccount);
    }

    @Override
    public Person getRemotePerson(String passportNumber) throws RemoteException {
        if (passportNumber == null) {
            return null;
        }
        return persons.get(passportNumber);
    }

}
