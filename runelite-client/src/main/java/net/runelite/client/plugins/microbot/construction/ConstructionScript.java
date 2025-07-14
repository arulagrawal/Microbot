package net.runelite.client.plugins.microbot.construction;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.NPC;
import net.runelite.api.SpriteID;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.ItemID;
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
    public static String version = "1.1";
    public Integer doorsBuilt = 0;
    public Integer doorsPerHour = 0;
    public Boolean servantsBagEmpty = false;
    public Boolean insufficientCoins = false;
    public Boolean butlerSent = false;
    public Boolean firstDoorCycle = true;
    public Integer doorsInCurrentCycle = 0;

    ConstructionState state = ConstructionState.Idle;

    public TileObject getOakDungeonDoorSpace() {
        return Rs2GameObject.findObjectById(15328); // ID for oak dungeon door space
    }

    public TileObject getOakDungeonDoor() {
        return Rs2GameObject.findObjectById(13344); // ID for oak dungeon door
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
        butlerSent = false;
        firstDoorCycle = true;
        doorsInCurrentCycle = 0;
        state = ConstructionState.Starting;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                Rs2Tab.switchToInventoryTab();
                calculateState();
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
        TileObject oakDoorSpace = getOakDungeonDoorSpace();
        TileObject oakDoor = getOakDungeonDoor();
        var butler = getButler();
        int plankCount = Rs2Inventory.itemQuantity(ItemID.PLANK_OAK);

        if (servantsBagEmpty && insufficientCoins) {
            state = ConstructionState.Stopped;
            Microbot.getNotifier().notify("Insufficient coins to pay butler!");
            Microbot.log("Insufficient coins to pay butler!");
            return;
        }

        // Strategy implementation
        if (firstDoorCycle) {
            // Starting from full inventory - send butler for 25 oak planks
            if (!butlerSent && butler != null) {
                state = ConstructionState.Butler;
                return;
            }

            // Build and remove 2 doors, leaving second one built
            if (oakDoor != null && doorsInCurrentCycle < 2) {
                // Always remove the door if we haven't completed 2 cycles yet
                if (doorsInCurrentCycle == 0) {
                    // Remove first door immediately after building
                    state = ConstructionState.Remove;
                } else if (doorsInCurrentCycle == 1) {
                    // Remove second door immediately after building
                    state = ConstructionState.Remove;
                }
            } else if (oakDoorSpace != null && oakDoor == null && plankCount >= 10) {
                // Build door if space is available and we have planks
                if (doorsInCurrentCycle < 2) {
                    state = ConstructionState.Build;
                } else {
                    // We've completed 2 build/remove cycles, now build final door and wait for butler
                    state = ConstructionState.Build;
                    // After this build, we'll wait for butler to return
                }
            } else if (doorsInCurrentCycle >= 2 && oakDoor != null) {
                // We have the second door built, wait for butler to return
                if (butler != null) {
                    firstDoorCycle = false;
                    doorsInCurrentCycle = 0;
                    state = ConstructionState.Remove; // Remove the door when butler returns
                } else {
                    state = ConstructionState.Idle; // Wait for butler
                }
            } else {
                state = ConstructionState.Idle;
            }
        } else {
            // Butler has returned - remove and build a door, then send butler away
            if (butler != null) {
                if (oakDoor != null) {
                    state = ConstructionState.Remove;
                } else if (oakDoorSpace != null && plankCount >= 10) {
                    state = ConstructionState.Build;
                } else {
                    // Send butler away and reset cycle
                    state = ConstructionState.Butler;
                    firstDoorCycle = true;
                    butlerSent = false;
                }
            } else {
                // Continue normal building cycle
                if (oakDoorSpace != null && oakDoor == null && plankCount >= 10) {
                    state = ConstructionState.Build;
                } else if (oakDoor != null) {
                    state = ConstructionState.Remove;
                } else {
                    state = ConstructionState.Idle;
                }
            }
        }

        if (oakDoorSpace == null && oakDoor == null) {
            state = ConstructionState.Idle;
            Microbot.getNotifier().notify("Looks like we are no longer in our house.");
            shutdown();
        }
    }

    private void remove() {
        TileObject oakDoor = getOakDungeonDoor();
        if (oakDoor == null) return;
        if (Rs2GameObject.interact(oakDoor, "Remove")) {
            Rs2Dialogue.sleepUntilInDialogue();

            // Butler spoke with us in the same tick/after we attempted to remove door
            if (!Rs2Dialogue.hasQuestion("Really remove it?")) {
                sleep(600);
                Rs2GameObject.interact(oakDoor, "Remove");
            }
            Rs2Dialogue.sleepUntilHasDialogueOption("Yes");
            Rs2Dialogue.keyPressForDialogueOption(1);
            sleepUntil(() -> getOakDungeonDoorSpace() != null, 1800);

            if (firstDoorCycle) {
                doorsInCurrentCycle++;
            }
        }
    }

    private void build() {
        TileObject oakDoorSpace = getOakDungeonDoorSpace();
        if (oakDoorSpace == null) {
            Microbot.log("could not find door space");
            return;
        }
        if (Rs2GameObject.interact(oakDoorSpace, "Build")) {
            sleepUntil(this::hasFurnitureInterfaceOpen, 1200);
            Rs2Keyboard.keyPress('1');
            sleepUntil(() -> getOakDungeonDoor() != null, 1200);
            if (getOakDungeonDoor() != null) {
                doorsBuilt++;
                // Don't increment doorsInCurrentCycle here - only in remove()
            }
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
            if(!Rs2Npc.interact(butler, "talk-to")) return;
        } else {
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

        if (Rs2Dialogue.hasQuestion("Repeat last task?")) {
            Rs2Dialogue.keyPressForDialogueOption(1);
            butlerSent = true;
            return;
        }

        if (Rs2Dialogue.hasSelectAnOption()) {
            if (Rs2Dialogue.hasDialogueOption("Go to the bank...")) {
                Rs2Dialogue.sleepUntilHasDialogueText("Dost thou wish me to exchange that certificate");
                Rs2Dialogue.clickContinue();
                Rs2Dialogue.sleepUntilSelectAnOption();
                Rs2Dialogue.keyPressForDialogueOption(1);
                Rs2Widget.sleepUntilHasWidget("Enter amount:");
                Rs2Keyboard.typeString("25"); // Request 25 oak planks as per strategy
                Rs2Keyboard.enter();
                Rs2Dialogue.clickContinue();
                butlerSent = true;
                return;
            }
        }

        if (Rs2Dialogue.hasDialogueText("must render unto me the 10,000 coins that are due")) {
            servantsBagEmpty = true;

            Rs2Dialogue.clickContinue();
            Rs2Dialogue.sleepUntilSelectAnOption();

            if (!Rs2Dialogue.hasDialogueOption("here's 10,000 coins")) {
                insufficientCoins = true;
                return;
            }

            Rs2Dialogue.keyPressForDialogueOption(1);
        }
    }
}
