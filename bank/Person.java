package info.kgeorgiy.ja.lorents.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Person extends Remote {

    /**
     * Returns the person's name.
     * @return {@link String} of person's name
     * @throws RemoteException if a communication-related exception occurs
     */
    String getFirstName() throws RemoteException;

    /**
     * Returns the person's surname.
     * @return {@link String} of person's surname
     * @throws RemoteException if a communication-related exception occurs
     */
    String getLastName() throws RemoteException;

    /**
     * Returns the person's passport number.
     * @return {@link String} of person's passport number
     * @throws RemoteException if a communication-related exception occurs
     */
    String getPassportNumber() throws RemoteException;

}
