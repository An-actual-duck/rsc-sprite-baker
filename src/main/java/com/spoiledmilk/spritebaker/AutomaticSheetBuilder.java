package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.util.List;

/** Shared zero-configuration animation discovery and 18-cell sheet setup. */
public final class AutomaticSheetBuilder {
    private AutomaticSheetBuilder(){}

    public static Result populate(SpriteProject project, AnimationWorkspace workspace) throws IOException {
        AnimationDiscovery.populateKnown(project, workspace);
        Sequence530 standingSequence = project.standingSequenceId < 0 ? null : workspace.cache.loadSequence(project.standingSequenceId);
        Sequence530 walkingSequence = project.walkingSequenceId < 0 ? null : workspace.cache.loadSequence(project.walkingSequenceId);
        PoseSelection[] movement = movementSuggestions(standingSequence, walkingSequence);
        for (int row = 0; row < movement.length; row++) {
            if (movement[row] != null) for (int column = 0; column < 5; column++) {
                project.sheet.suggest(row, column, movement[row]);
            }
        }

        List<CombatCandidate> candidates = AnimationDiscovery.combatCandidates(workspace);
        CombatCandidate selected = AnimationDiscovery.chooseAutomatic(candidates);
        if (selected != null) {
            project.combatSequenceId = selected.sequenceId;
            PoseSelection[] combat = selected.suggestions();
            for (int row = 0; row < TargetSheet.ROWS; row++) {
                combat[row].source = "automatic-combat-candidate";
                project.sheet.suggest(row, TargetSheet.COLUMNS - 1, combat[row]);
            }
            return new Result(candidates.size(), false, selected.score, selected.reason);
        }
        for (int row = 0; row < TargetSheet.ROWS; row++) {
            if (movement[row] != null) project.sheet.suggest(row, TargetSheet.COLUMNS - 1, movement[row]);
        }
        return new Result(0, true, 0, "no compatible combat candidate; movement poses retained");
    }

    static PoseSelection[] movementSuggestions(Sequence530 standingSequence, Sequence530 walkingSequence) {
        PoseSelection standing = standingSequence == null ? null : AutomaticPoseSuggestions.standing(standingSequence);
        if (standing == null && walkingSequence != null) {
            standing = AutomaticPoseSuggestions.standing(walkingSequence);
            standing.source = "automatic-walking-rest-fallback";
        } else if (standing != null) standing.source = "automatic-default";
        PoseSelection left = walkingSequence == null ? standing : AutomaticPoseSuggestions.leftStep(walkingSequence);
        PoseSelection right = walkingSequence == null ? standing : AutomaticPoseSuggestions.rightStep(walkingSequence);
        if (left != null && left != standing) left.source = "automatic-default";
        if (right != null && right != standing) right.source = "automatic-default";
        return new PoseSelection[]{standing, left, right};
    }

    public static final class Result {
        public final int combatCandidateCount;
        public final boolean movementCombatFallback;
        public final int combatScore;
        public final String combatReason;

        Result(int combatCandidateCount, boolean movementCombatFallback, int combatScore, String combatReason) {
            this.combatCandidateCount = combatCandidateCount;
            this.movementCombatFallback = movementCombatFallback;
            this.combatScore = combatScore;
            this.combatReason = combatReason;
        }
    }
}
