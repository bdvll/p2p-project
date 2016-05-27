/*
 * 2016 Royal Institute of Technology (KTH)
 *
 * LSelector is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.news.sim;

import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.run.LauncherComp;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SimLauncher {
    public static void main(String[] args) {
        for(int i = 0; i <= 4; ++i) {
            for(int j = 0; j <= 2; ++j){
                DisseminationConfig.saveParams(getDissRate(j), getNodeCount(i));
                SimulationScenario.setSeed(ScenarioSetup.scenarioSeed);
                SimulationScenario simpleBootScenario = ScenarioGen.simpleBoot();
                simpleBootScenario.simulate(LauncherComp.class);
            }
        }
    }

    private static int getNodeCount(int i){
        switch (i){
            case 0: return 25;
            case 1: return 50;
            case 2: return 200;
            case 3: return 500;
            case 4: return 1000;
            default: return 1;
        }
    }

    private static int getDissRate(int i){
        switch (i){
            case 0: return 5000;
            case 1: return 2500;
            case 2: return 1000;
            default: return 1;
        }
    }
}
