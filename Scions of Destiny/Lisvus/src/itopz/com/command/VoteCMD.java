/*
 * Copyright (c) 2021 iTopZ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package itopz.com.command;

import itopz.com.model.IndividualResponse;
import itopz.com.gui.Gui;
import itopz.com.model.entity.ITOPZ_INDIVIDUAL;
import itopz.com.util.URL;
import itopz.com.util.Utilities;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;

/**
 * @Author Nightwolf
 * iToPz Discord: https://discord.gg/KkPms6B5aE
 * @Author Rationale
 * Base structure credits goes on Rationale Discord: Rationale#7773
 *
 * Vote Donation System
 * Script website: https://itopz.com/
 * Script version: 1.0
 * Pack Support: Lisvus Scions of Destiny rev 728
 *
 * Personal Donate Panels: https://www.denart-designs.com/
 * Free Donate panel: https://itopz.com/
 */
public class VoteCMD implements IVoicedCommandHandler
{
	// local response variables
	private int responseCode;
	private boolean hasVoted;
	private long serverTime;
	private long voteTime;
	private String responseError;
	private String _IPAddress;

	// 12 hour reuse
	private final long VOTE_REUSE = 43200 * 1000;

	// commands
	private final String[] COMMANDS =
	{
		"itopz",
	};

	@Override
	public boolean useVoicedCommand(String s, L2PcInstance player, String s1)
	{
		// check the ip (local ranges will not be allowed)
		if (!playerChecks(player, COMMANDS[0]))
		{
			player.sendPacket(new ActionFailed());
			return false;
		}

		getVoteInfo();

		// player can get reward?
		if (isEligible(player, COMMANDS[0]))
		{
			sendMsg(player, "Successfully voted in " + COMMANDS[0] + "!");
			// set var can vote: 12 hours (in ms).
			Utilities.saveIndividualVar(player, COMMANDS[0], COMMANDS[0] + "_can_vote", System.currentTimeMillis() + VOTE_REUSE);
			player.sendPacket(new ActionFailed());
			return ITOPZ_INDIVIDUAL.reward(player);
		}
		return false;
	}

	/**
	 * Validate user
	 *
	 * @param player object
	 * @param topsite string
	 * @return boolean
	 */
	private boolean playerChecks(L2PcInstance player, String topsite)
	{
		// check for private network (website will not accept it)
		if (player.getClient().getConnection().getInetAddress() == null || Utilities.localIp(player.getClient().getConnection().getInetAddress()))
		{
			sendMsg(player, "Private networks are not allowed.");
			return false;
		}

		// check if 12 hours has pass from last vote
		long voteTimer = Utilities.selectIndividualVar(player, topsite + "_can_vote");
		if (voteTimer > System.currentTimeMillis())
		{
			String dateFormatted = Utilities.formatMillisecond(voteTimer);
			sendMsg(player, "You already voted on " + topsite + " try again after " + dateFormatted + ".");
			return false;
		}

		_IPAddress = player.getClient().getConnection().getInetAddress().getHostAddress();
		return true;
	}

	/**
	 * get info for this player
	 */
	private void getVoteInfo()
	{
		new Thread(() ->
		{
			// get response from itopz about this ip address
			IndividualResponse response = IndividualResponse.OPEN(URL.ITOPZ_INDIVIDUAL_URL.toString(), _IPAddress).connect();
			// set variables
			responseCode = response.getResponseCode();
			hasVoted = response.hasVoted();
			voteTime = response.getVoteTime() * 1000;
			serverTime = response.getServerTime() * 1000;
			responseError = response.getError();
		}).run();
	}

	/**
	 * Return true if player is eligible to get a reward
	 *
	 * @param player object
	 * @return boolean
	 */
	private boolean isEligible(L2PcInstance player, String topsite)
	{
		// check if response was not ok
		if (responseCode != 200)
		{
			Gui.getInstance().ConsoleWrite(topsite.toUpperCase() + " Response Code:" + responseCode);
			sendMsg(player, topsite.toUpperCase() + " server is not responding try again later.");
			return false;
		}

		// server returned error
		if (responseError != null)
		{
			Gui.getInstance().ConsoleWrite(topsite.toUpperCase() + " Response Code:" + responseError);
			sendMsg(player, "Server error:" + responseError + ".");
			return false;
		}

		// player has not voted
		if (!hasVoted)
		{
			sendMsg(player, "You didnt vote at " + topsite.toUpperCase() + ".");
			return false;
		}

		// check 12hours on server time pass
		if (voteTime + VOTE_REUSE < serverTime)
		{
			sendMsg(player, "The reward has expired, vote again.");
			return false;
		}

		// the player is eligible to receive reward
		return true;
	}

	/**
	 * Send message to player
	 *
	 * @param player object
	 * @param s string
	 */
	private void sendMsg(L2PcInstance player, String s)
	{
		player.sendMessage(s);
	}


	@Override
	public String[] getVoicedCommandList()
	{
		return COMMANDS;
	}
}