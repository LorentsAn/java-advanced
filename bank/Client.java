package info.kgeorgiy.ja.lorents.bank;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Objects;

public final class Client {

    private final static int PORT = 8888;
    private final static String HOST = "//localhost/bank";
    private final static String USAGE = "[name] [surname] [passport number] [account number] [change in the amount]";

    private static boolean validateArgs(String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Incorrect Arguments, use:" + USAGE);
            return false;
        }
        try {
            Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("Account change amount argument should be a number" + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Imitates the work of the client on the passed or default port.
     * @param args the first argument is the {@link String} of port.
     */
    public static void main(final String... args) throws RemoteException {
        final Bank bank;

        try {
            Registry registry = LocateRegistry.getRegistry(PORT);
            bank = (Bank) registry.lookup(HOST);
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        }

        if (!validateArgs(args)) {
            return;
        }

        final String firstName = args[0];
        final String lastName = args[1];
        final String passportNumber = args[2];
        final String id = args[3];
        final int amount = Integer.parseInt(args[4]);
        if (bank.getRemotePerson(id) == null) {
            bank.createPerson(passportNumber, firstName, lastName);
        }

        Person person = bank.getRemotePerson(id);

        if (bank.getAccount(firstName, person) == null) {
            System.out.println("Creating account");
            bank.createAccount(firstName, person);
        }
        Account account = bank.getAccount(firstName, person);

        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + amount);
        System.out.println("Money: " + account.getAmount());
    }
}
