package net.runelite.client.plugins.microbot.construction;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.SpriteID;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.construction.enums.ConstructionState;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.concurrent.TimeUnit;


public class ConstructionScript extends Script {
    public static String version = "1.2"; // Updated version for Oak Doors
    public Integer doorsBuilt = 0;
    public Integer doorsPerHour = 0;
    public Boolean servantsBagEmpty = false;
    public Boolean insufficientCoins = false;

    ConstructionState state = ConstructionState.Idle;

    // Returns the door space object. ID 15328 is for a "Door hotspot".
    public TileObject getOakDoorSpace() {
        return Rs2GameObject.getWallObject(15328);
    }

    // Returns the built Oak Door object. ID 15305 is for an "Oak door".
    // This ID may need to be verified depending on the house style.
    public TileObject getOakDoor() {
        return Rs2GameObject.getWallObject(13344);
    }

    public Rs2NpcModel getButler() {
        return Rs2Npc.getNpc("Demon butler");
    }

    public boolean hasFurnitureInterfaceOpen() {
        return Rs2Widget.findWidget("Furniture", null) != null;
    }

    public boolean run(ConstructionConfig config) {
        doorsBuilt = 0;
        doorsPerHour = 0;
        servantsBagEmpty = false;
        insufficientCoins = false;
        state = ConstructionState.Starting;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                Rs2Tab.switchToInventoryTab();
                calculateState();
                Microbot.log("got new state");
                if (state == ConstructionState.Build) {
                    build();
                } else if (state == ConstructionState.Remove) {
                    remove();
                } else if (state == ConstructionState.Butler) {
                    butler();
                } else if (state == ConstructionState.Stopped) {
                    servantsBagEmpty = false;
                    insufficientCoins = false;
                    Microbot.stopPlugin(Microbot.getPlugin("ConstructionPlugin"));
                    shutdown();
                }
            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private void calculateState() {
        TileObject doorSpace = getOakDoorSpace();
        TileObject door = getOakDoor();
        var butler = getButler();
        // Use OAK_PLANK ID 8778
        int plankCount = Rs2Inventory.itemQuantity(ItemID.PLANK_OAK);

        if (servantsBagEmpty && insufficientCoins) {
            state = ConstructionState.Stopped;
            Microbot.getNotifier().notify("Insufficient coins to pay butler!");
            Microbot.log("Insufficient coins to pay butler!");
            return;
        }

        if (door != null) {
            // If the door is built, we need to remove it.
            state = ConstructionState.Remove;
        } else if (doorSpace != null) {
            // If we have a door space, check if we have planks to build.
            if (plankCount >= 10) {
                state = ConstructionState.Build;
            } else {
                // Not enough planks, call the butler if he's around.
                state = (butler != null) ? ConstructionState.Butler : ConstructionState.Idle;
            }
        } else {
            Microbot.log("couldn't find door or door space?");
            // No door and no door space found.
            state = ConstructionState.Idle;
            Microbot.getNotifier().notify("Could not find a door or door space. Make sure you are in your house.");
            // Consider shutting down if this state persists.
            // shutdown();
        }
    }

    private void build() {
        TileObject doorSpace = getOakDoorSpace();
        if (doorSpace == null) return;
        if (Rs2GameObject.interact(doorSpace, "Build")) {
            sleepUntil(this::hasFurnitureInterfaceOpen, 1500);
            // Press '4' for Oak Door. This might need to be changed if the menu is different.
            Rs2Keyboard.keyPress('1');
            sleepUntil(() -> getOakDoor() != null, 1500);
            if (getOakDoor() != null)
            {
                doorsBuilt++;
            }
        }
    }

    private void remove() {
        TileObject door = getOakDoor();
        if (door == null) return;
        if (Rs2GameObject.interact(door, "Remove")) {
            Rs2Dialogue.sleepUntilInDialogue();

            // Butler might interrupt, so re-attempt if the confirmation dialogue isn't found.
            if (!Rs2Dialogue.hasQuestion("Really remove it?"))
            {
                sleep(600);
                Rs2GameObject.interact(door, "Remove");
            }
            Rs2Dialogue.sleepUntilHasDialogueOption("Yes");
            Rs2Dialogue.keyPressForDialogueOption(1); // Press '1' for "Yes"
            sleepUntil(() -> getOakDoorSpace() != null, 1800);
        }
    }

    private void butler() {
        var butler = getButler();
        boolean butlerIsTooFar;
        if (butler == null) return;

        butlerIsTooFar = Microbot.getClientThread().runOnClientThreadOptional(() -> {
            int distance = butler.getWorldLocation().distanceTo(Microbot.getClient().getLocalPlayer().getWorldLocation());
            return distance > 3;
        }).orElse(false);

        if (!butlerIsTooFar) {
            Rs2Npc.interact(butler, "talk-to");
        } else {
            // If butler is not nearby, call him using the house options.
            Rs2Tab.switchToSettingsTab();
            sleep(800, 1800);
            Widget houseOptionWidget = Rs2Widget.findWidget(SpriteID.OPTIONS_HOUSE_OPTIONS, null);
            if (houseOptionWidget != null)
                Microbot.getMouse().click(houseOptionWidget.getCanvasLocation());
            sleep(800, 1800);
            Widget callServantWidget = Rs2Widget.findWidget("Call Servant", null);
            if (callServantWidget != null)
                Microbot.getMouse().click(callServantWidget.getCanvasLocation());
        }

        Rs2Dialogue.sleepUntilInDialogue();

        // Handle payment first if required
        if (Rs2Dialogue.hasDialogueText("must render unto me the 10,000 coins that are due")) {
            servantsBagEmpty = true;
            Rs2Dialogue.clickContinue();
            Rs2Dialogue.sleepUntilSelectAnOption();
            if (!Rs2Dialogue.hasDialogueOption("here's 10,000 coins")) {
                insufficientCoins = true;
                return;
            }
            Rs2Dialogue.keyPressForDialogueOption(1);
            return;
        }

        // Most efficient path: repeat the last task (fetching 20 oak planks).
        if (Rs2Dialogue.hasQuestion("Repeat last task?")) {
            Rs2Dialogue.keyPressForDialogueOption(1);
            sleep(600); // Small delay to ensure the action is registered
            return;
        }

        // Fallback for first-time setup or if the "repeat" dialogue is missed.
        // This instructs the butler to fetch 20 of an item from the bank.
        // NOTE: The user MUST manually ask the butler to fetch 20 oak planks once for the "Repeat" option to work.
        if (Rs2Dialogue.hasSelectAnOption()) {
            if (Rs2Dialogue.hasDialogueOption("Go to the bank...")) {
                Microbot.log("Attempting to ask butler to fetch planks from bank.");
                Microbot.log("Please manually ask your butler to fetch 20 Oak Planks ONCE.");
                Microbot.log("The script will then use the 'Repeat last task' option.");
                // The following is an example flow and may need adjustment
                Rs2Dialogue.keyPressForDialogueOption(1); // "Go to the bank..."
                Rs2Dialogue.sleepUntilSelectAnOption();
                Rs2Dialogue.keyPressForDialogueOption(1); // "Fetch from bank..."
                Rs2Widget.sleepUntilHasWidget("Oak plank"); // Wait for item list
                // click on "Oak plank" - This part is complex and best done manually once.
                // Rs2Widget.clickWidget("Oak plank");
                // Rs2Keyboard.typeString("20");
                // Rs2Keyboard.enter();
                return;
            }
        }
    }
}
