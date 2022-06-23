package info.kgeorgiy.ja.lorents.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it is not already exists.
     * @param subId account id
     * @return created or existing account.
     */
    boolean createAccount(String subId, Person person) throws RemoteException;

    /**
     * Returns account by identifier.
     * @param subId account id
     * @return account with specified identifier or {@code null} if such account does not exists.
     */
    Account getAccount(String subId, Person person) throws RemoteException;

    /**
     * Creates a person if it hasn't already been created
     * @param firstName {@link String}
     * @param LastName {@link String}
     * @param passportNumber {@link String}
     * @return created or existing person.
     */
    boolean createPerson(String firstName, String LastName, String passportNumber) throws RemoteException;

    /**
     * Returns a local person by passport number.
     * @param passportNumber {@link String}
     * @return the local person, or {@code null} if there is no record of the person
     */
    Person getLocalPerson(String passportNumber) throws RemoteException;

    /**
     * Returns a remote person by passport number.
     * @param passportNumber {@link String}
     * @return the remote person, or {@code null} if there is no record of the person
     */
    Person getRemotePerson(String passportNumber) throws RemoteException;

    /**
     * Returns all account ids of a person.
     * @param person {@link Person}
     * @return {@code Set<String>} of all account ids of a person, or {@code null} if there is no record of the person.
     */
    Set<String> getAccountsOfPerson(Person person) throws RemoteException;
}
