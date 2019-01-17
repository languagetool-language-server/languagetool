/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2018, MPL Adam Voss vossad01@gmail.com
 *
 * In memory of Adam Voss, original creator
 * July 11, 1991 - July 11, 2018
 * https://github.com/adamvoss
 * http://schluterbalikfuneralhome.com/obituary/adam-voss
 *
 */
package org.languagetool.languageserver;

import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DocumentPositionCalculatorTest {

    public final DocumentPositionCalculator sut = new DocumentPositionCalculator("Hello\n" +
            "Enthusiastic\r\n" +
            "Reader!");

    @Test
    public void testGetPosition_first_character() {
        Position position = sut.getPosition(0);

        Assertions.assertEquals(new Position(0, 0), position);
    }

    @Test
    public void testGetPositionStartsWithNewline() {
        Position position = new DocumentPositionCalculator("\nHi").getPosition(1);

        Assertions.assertEquals(new Position(1, 0), position);
    }

    @Test
    public void testGetPositionSecondLineStart() {
        Position position = sut.getPosition(6);

        Assertions.assertEquals(new Position(1, 0), position);
    }

    @Test
    public void testGetPositionSecondLineSecondCharacter() {
        Position position = sut.getPosition(7);

        Assertions.assertEquals(new Position(1, 1), position);
    }

    @Test
    public void testGetPositionArbitraryPosition() {
        Position position = sut.getPosition(12);

        Assertions.assertEquals(new Position(1, 6), position);
    }
}
