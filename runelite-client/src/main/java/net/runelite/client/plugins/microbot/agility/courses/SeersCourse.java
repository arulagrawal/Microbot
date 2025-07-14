package net.runelite.client.plugins.microbot.agility.courses;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.agility.models.AgilityObstacleModel;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

public class SeersCourse implements AgilityCourseHandler
{
	@Override
	public WorldPoint getStartPoint()
	{
		return new WorldPoint(2729, 3486, 0);
	}

	@Override
	public List<AgilityObstacleModel> getObstacles()
	{
		return List.of(
			new AgilityObstacleModel(ObjectID.ROOFTOPS_SEERS_WALLCLIMB),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_SEERS_JUMP),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_SEERS_TIGHTROPE),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_SEERS_JUMP_1),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_SEERS_JUMP_2),
			new AgilityObstacleModel(ObjectID.ROOFTOPS_SEERS_LEAPDOWN)
		);
	}

	public boolean teleportToSeers() {
		if (!Rs2Magic.canCast(MagicAction.CAMELOT_TELEPORT)) {
			return false;
		}
		Rs2Magic.cast(MagicAction.CAMELOT_TELEPORT, "Seers'", 2);

		return true;
	}

	@Override
	public boolean handleWalkToStart(WorldPoint playerWorldLocation)
	{
		if (Microbot.getClient().getTopLevelWorldView().getPlane() != 0)
		{
			return false;
		}

		if (getCurrentObstacleIndex() > 0)
		{
			return false;
		}

		int distToStart = playerWorldLocation.distanceTo(getStartPoint());
		if (distToStart > 15)
		{
			Microbot.log("teleporting to seers");
			if (this.teleportToSeers()) {
				Microbot.log("teleported to seers");
			} else {
				Microbot.log("error teleporting to seers");
			}
			Global.sleepUntil(() -> playerWorldLocation.distanceTo(getStartPoint()) < 15, 3000);
			//Rs2Walker.walkTo(getStartPoint(), 2);
			return true;
		}
		return false;
	}

	@Override
	public Integer getRequiredLevel()
	{
		return 60;
	}
}
