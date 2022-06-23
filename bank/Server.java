package info.kgeorgiy.ja.lorents.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public final class Server {

    private final static int DEFAULT_PORT = 8888;
    private final static String HOST = "//localhost/bank";

    /**
     *  Starts {@link Server} on starts the server on the passed or default port.
     * @param args the first argument is the {@link String} of port.
     */
    public static void main(final String... args) {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Bank bank = new RemoteBank(port);
        try {
            Bank stub = (Bank) UnicastRemoteObject.exportObject(bank, port);
            Registry registry = LocateRegistry.createRegistry(port);
            Naming.rebind(HOST, stub);
            System.out.println("Server started");
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
        } catch (final MalformedURLException e) {
            System.out.println("Malformed URL");
        }
    }
}
