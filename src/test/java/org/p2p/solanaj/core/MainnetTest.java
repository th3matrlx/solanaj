package org.p2p.solanaj.core;

import org.bitcoinj.core.Utils;
import org.junit.Ignore;
import org.junit.Test;
import org.p2p.solanaj.programs.MemoProgram;
import org.p2p.solanaj.programs.SystemProgram;
import org.p2p.solanaj.rpc.Cluster;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.*;
import org.p2p.solanaj.serum.*;
import org.p2p.solanaj.token.TokenManager;
import org.p2p.solanaj.utils.ByteUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;

public class MainnetTest extends AccountBasedTest {

    private final RpcClient client = new RpcClient(Cluster.MAINNET);
    private final PublicKey publicKey = solDestination;
    public final TokenManager tokenManager = new TokenManager(client);
    private final SerumManager serumManager = new SerumManager(client);

    private static final PublicKey USDC_TOKEN_MINT = new PublicKey("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v");

    @Test
    public void getAccountInfoBase64() {
        try {
            // Get account Info
            final AccountInfo accountInfo = client.getApi().getAccountInfo(PublicKey.valueOf("So11111111111111111111111111111111111111112"));
            final double balance = (double) accountInfo.getValue().getLamports()/ 100000000;

            // Account data list
            final List<String> accountData = accountInfo.getValue().getData();

            // Verify "base64" string in accountData
            assertTrue(accountData.stream().anyMatch(s -> s.equalsIgnoreCase("base64")));
            assertTrue(balance > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAccountInfoBase58() {
        try {
            // Get account Info
            final AccountInfo accountInfo = client.getApi().getAccountInfo(PublicKey.valueOf("So11111111111111111111111111111111111111112"), Map.of("encoding", "base58"));
            final double balance = (double) accountInfo.getValue().getLamports()/ 100000000;

            // Account data list
            final List<String> accountData = accountInfo.getValue().getData();

            // Verify "base64" string in accountData
            assertTrue(accountData.stream().anyMatch(s -> s.equalsIgnoreCase("base58")));
            assertTrue(balance > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAccountInfoRootCommitment() {
        try {
            // Get account Info
            final AccountInfo accountInfo = client.getApi().getAccountInfo(PublicKey.valueOf("So11111111111111111111111111111111111111112"), Map.of("commitment", "root"));
            final double balance = (double) accountInfo.getValue().getLamports()/ 100000000;

            // Verify any balance
            assertTrue(balance > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getAccountInfoJsonParsed() {
        try {
            final SplTokenAccountInfo accountInfo = client.getApi().getSplTokenAccountInfo(
                    PublicKey.valueOf("8tnpAECxAT9nHBqR1Ba494Ar5dQMPGhL31MmPJz1zZvY")
            );

            assertTrue(
                    accountInfo.getValue().getData().getProgram().equalsIgnoreCase("spl-token")
            );

        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    /**
     * Uses a {@link MarketBuilder} class to retrieve data about the BTC/USDC Serum market.
     */
    @Test
    public void marketBuilderBtcUsdcTest() {
        // Pubkey of SRM/USDC market
        final PublicKey publicKey = new PublicKey("ByRys5tuUWDgL73G8JBAEfkdFf8JWBzPBDHsBVQ5vbQA"); //SRM/USDC

        final Market solUsdcMarket = new MarketBuilder()
                .setClient(client)
                .setPublicKey(publicKey)
                .setRetrieveOrderBooks(true)
                .build();

        final OrderBook bids = solUsdcMarket.getBidOrderBook();
        final OrderBook asks = solUsdcMarket.getAskOrderBook();

        LOGGER.info("Best bid = " + bids.getBestBid().getPrice() / 1000.0);
        LOGGER.info("Best ask = " + asks.getBestAsk().getPrice() / 1000.0);

        // Verify at least 1 bid and 1 ask (should always be for BTC/USDC)
        assertTrue(bids.getOrders().size() > 0);
        assertTrue(asks.getOrders().size() > 0);
    }

    /**
     * Verifies that {@link OrderBook} headers are properly read by {@link OrderBook#readOrderBook(byte[])}
     */
    @Test
    public void orderBookTest() {
        byte[] data = new byte[0];

        try {
            data = Files.readAllBytes(Paths.get("src/test/resources/orderbook.bin"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        OrderBook bidOrderBook = OrderBook.readOrderBook(data);

        LOGGER.info(bidOrderBook.getAccountFlags().toString());

        Slab slab = bidOrderBook.getSlab();

        assertNotNull(slab);
        assertEquals(141, slab.getBumpIndex());
        assertEquals(78, slab.getFreeListLen());
        assertEquals(56, slab.getFreeListHead());
        assertEquals(32, slab.getLeafCount());
    }

    /**
     * Will verify {@link ByteUtils} or {@link SerumUtils} can read seqNum and price.
     * Currently just reads price and logs it.
     */
    @Test
    public void testPriceDeserialization() {
        /* C:\apps\solanaj\lqidusdc.bin (1/12/2021 8:55:59 AM)
   StartOffset(d): 00001277, EndOffset(d): 00001292, Length(d): 00000016 */

        byte[] rawData = {
                (byte)0xDB, (byte)0xFE, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xFF, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        };

        long seqNum = Utils.readInt64(rawData, 0);
        long price = Utils.readInt64(rawData, 8);

        LOGGER.info("Price = " + price);
        LOGGER.info("seqNum = " + seqNum);

        assertEquals(1, price);
        assertEquals(seqNum, -293L);
    }

    /**
     * Uses a {@link MarketBuilder} class to retrieve data about the SOL/USDC Serum market.
     */
    @Test
    public void marketBuilderSolUsdcTest() {
        final PublicKey solUsdcPublicKey = new PublicKey("9wFFyRfZBsuAha4YcuxcXLKwMxJR43S7fPfQLusDBzvT");

        final Market solUsdcMarket = new MarketBuilder()
                .setClient(client)
                .setPublicKey(solUsdcPublicKey)
                .setRetrieveOrderBooks(true)
                .build();

        final OrderBook bids = solUsdcMarket.getBidOrderBook();
        final OrderBook asks = solUsdcMarket.getAskOrderBook();
        LOGGER.info("Market = " + solUsdcMarket.toString());

        final ArrayList<Order> asksOrders = asks.getOrders();
        asksOrders.sort(Comparator.comparingLong(Order::getPrice).reversed());
        asksOrders.forEach(order -> {
            System.out.printf("SOL/USDC Ask: $%.4f (Quantity: %.4f)%n", order.getFloatPrice(), order.getFloatQuantity());
        });

        LOGGER.info("Bids");

        final ArrayList<Order> orders = bids.getOrders();
        orders.sort(Comparator.comparingLong(Order::getPrice).reversed());
        orders.forEach(order -> {
            System.out.printf("SOL/USDC Bid: $%.4f (Quantity: %.4f)%n", order.getFloatPrice(), order.getFloatQuantity());
        });

        // Verify that an order exists
        assertTrue(orders.size() > 0);
    }

    /**
     * Uses a {@link MarketBuilder} class to retrieve the Event Queue from the SOL/USDC Serum market.
     */
    @Test
    public void marketBuilderEventQueueTest() {
        final PublicKey solUsdcPublicKey = new PublicKey("2Pbh1CvRVku1TgewMfycemghf6sU9EyuFDcNXqvRmSxc");

        final MarketBuilder solUsdcMarketBuilder = new MarketBuilder()
                .setPublicKey(solUsdcPublicKey)
                .setClient(client)
                .setRetrieveOrderBooks(true)
                .setRetrieveEventQueue(true);

        Market solUsdcMarket = solUsdcMarketBuilder.build();

        LOGGER.info("Market = " + solUsdcMarket.toString());
        LOGGER.info("Event Queue = " + solUsdcMarket.getEventQueue());
        LOGGER.info("# of top traders = " + solUsdcMarket.getEventQueue().getTopTraders().size());
        LOGGER.info("# of Open Orders accounts = " + solUsdcMarket.getEventQueue().getOpenOrdersAccounts().size());

        List<TradeEvent> tradeEvents = solUsdcMarket.getEventQueue().getEvents();

        tradeEvents.forEach(tradeEvent -> {
            LOGGER.info(tradeEvent.toString());
        });

        //String transactionId = serumManager.consumeEvents(solUsdcMarket, testAccount, solUsdcMarket.getEventQueue().getOpenOrdersAccounts().stream().limit(5).collect(Collectors.toList()));

//        List<Order> bids = solUsdcMarket.getBidOrderBook().getOrders();
//
//        for (int i = 0; i < bids.size(); i++) {
//            LOGGER.info(String.format("Bid: %s", bids.get(i)));
//        }
//
//        LOGGER.info("Reloading market");
//        solUsdcMarket.reload(solUsdcMarketBuilder);

    }

    /**
     * Calls sendTransaction with a call to the Memo program included.
     */
    @Test
    @Ignore
    public void transactionMemoTest() {
        final int lamports = 1337;
        final PublicKey destination = solDestination;

        // Create account from private key
        final Account feePayer = testAccount;

        final Transaction transaction = new Transaction();
        transaction.addInstruction(
                SystemProgram.transfer(
                        feePayer.getPublicKey(),
                        destination,
                        lamports
                )
        );

        // Add instruction to write memo
        transaction.addInstruction(
                MemoProgram.writeUtf8(feePayer.getPublicKey(),"Hello from SolanaJ :)")
        );

        // Call sendTransaction
        String result = null;
        try {
            result = client.getApi().sendTransaction(transaction, feePayer);
            LOGGER.info("Result = " + result);
        } catch (RpcException e) {
            e.printStackTrace();
        }

        assertNotNull(result);
    }

    @Test
    public void getBlockCommitmentTest() {
        // Block 5 used for testing - matches docs
        long block = 5;

        try {
            final BlockCommitment blockCommitment = client.getApi().getBlockCommitment(block);

            LOGGER.info(String.format("block = %d, totalStake = %d", block, blockCommitment.getTotalStake()));

            assertNotNull(blockCommitment);
            assertTrue(blockCommitment.getTotalStake() > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getClusterNodesTest() {
        try {
            final List<ClusterNode> clusterNodes = client.getApi().getClusterNodes();

            // Make sure we got some nodes
            assertNotNull(clusterNodes);
            assertTrue(clusterNodes.size() > 0);

            // Output the nodes
            LOGGER.info("Cluster Nodes:");
            clusterNodes.forEach(clusterNode -> {
                LOGGER.info(clusterNode.toString());
            });
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getEpochInfoTest() {
        try {
            final EpochInfo epochInfo = client.getApi().getEpochInfo();
            assertNotNull(epochInfo);

            LOGGER.info(epochInfo.toString());

            // Validate the returned data
            assertTrue(epochInfo.getAbsoluteSlot() > 0);
            assertTrue(epochInfo.getEpoch() > 0);
            assertTrue(epochInfo.getSlotsInEpoch() > 0);
            assertTrue(epochInfo.getBlockHeight() > 0);
            assertTrue(epochInfo.getSlotIndex() > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getEpochScheduleTest() {
        try {
            final EpochSchedule epochSchedule = client.getApi().getEpochSchedule();
            assertNotNull(epochSchedule);

            LOGGER.info(epochSchedule.toString());

            // Validate the returned data
            assertTrue(epochSchedule.getSlotsPerEpoch() > 0);
            assertTrue(epochSchedule.getLeaderScheduleSlotOffset() > 0);
        } catch (RpcException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void sendTokenTest() {
        final PublicKey source = usdcSource; // Private key's USDC token account
        final PublicKey destination = usdcDestination; // Destination's USDC account
        final int tokenAmount = 10; // 0.000100 USDC

        // Create account from private key
        final Account owner = testAccount;

        // "10" = 0.0000001 (or similar)
        final String txId = tokenManager.transfer(
                owner,
                source,
                destination,
                USDC_TOKEN_MINT,
                tokenAmount
        );

        assertNotNull(txId);
    }

    @Test
    @Ignore
    public void transferCheckedTest() {
        final PublicKey source = usdcSource; // Private key's USDC token account
        final PublicKey destination = solDestination;

        /*
            amount = "0.0001" usdc
            amount = 100
            decimals = 6
         */

        final long tokenAmount = 100;
        final byte decimals = 6;

        // Create account from private key
        final Account owner = testAccount;

        final String txId = tokenManager.transferCheckedToSolAddress(
                owner,
                source,
                destination,
                USDC_TOKEN_MINT,
                tokenAmount,
                decimals
        );

        // TODO - actually verify something
        assertNotNull(txId);
    }

    @Test
    @Ignore
    public void initializeAccountTest() {
        final Account owner = testAccount;
        final Account newAccount = new Account();
        final String txId = tokenManager.initializeAccount(
                newAccount,
                USDC_TOKEN_MINT,
                owner
        );

        // TODO - actually verify something
        assertNotNull(txId);
        System.out.println(testAccount.getPublicKey().toBase58());
    }

    @Test
    @Ignore
    public void getConfirmedTransactionTest() {
        String txId = "46VcVPoecvVASnX9vHEZLA8JMS6BVXhvMMhqtGBcn9eg4bHehK6uA2icuTjwjWLZxwfxdT2z1CqYxCHHvjorvWDi";

        ConfirmedTransaction confirmedTransaction = null;
        try {
            confirmedTransaction = client.getApi().getConfirmedTransaction(txId);
        } catch (RpcException e) {
            e.printStackTrace();
        }
        //
        if (confirmedTransaction != null) {
            LOGGER.info(String.format("Tx: %s", confirmedTransaction));
        }


    }
}