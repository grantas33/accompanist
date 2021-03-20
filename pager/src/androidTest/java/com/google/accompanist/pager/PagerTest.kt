/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.accompanist.pager

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.width
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val MediumSwipeDistance = 0.8f
private const val ShortSwipeDistance = 0.6f

private const val FastVelocity = 4000f
private const val SlowVelocity = 400f

@OptIn(ExperimentalPagerApi::class) // Pager is currently experimental
@LargeTest
@RunWith(Parameterized::class)
class PagerTest(
    private val itemWidthFraction: Float,
    private val offscreenLimit: Int,
    private val layoutDirection: LayoutDirection,
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            // itemWidthFraction, offscreenLimit, layoutDirection

            // Test typical full-width items
            arrayOf(1f, 2, LayoutDirection.Ltr),
            arrayOf(1f, 2, LayoutDirection.Rtl),

            // Test an increased offscreenLimit
            arrayOf(1f, 4, LayoutDirection.Ltr),
            arrayOf(1f, 4, LayoutDirection.Rtl),

            // Test items with 80% widths
            arrayOf(0.8f, 2, LayoutDirection.Ltr),
            arrayOf(0.8f, 2, LayoutDirection.Rtl),
        )
    }

    @Test
    fun layout() {
        setPagerContent(
            layoutDirection = layoutDirection,
            pageModifier = Modifier.fillMaxWidth(itemWidthFraction),
            pageCount = 10,
            offscreenLimit = offscreenLimit,
        )

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        assertPagerLayout(
            currentPage = 0,
            pageCount = 10,
            offscreenLimit = offscreenLimit,
            expectedItemWidth = rootBounds.width * itemWidthFraction,
            layoutDirection = layoutDirection,
        )
    }

    @Test
    fun swipe() {
        setPagerContent(
            layoutDirection = layoutDirection,
            pageModifier = Modifier.fillMaxWidth(itemWidthFraction),
            pageCount = 10,
            offscreenLimit = offscreenLimit,
        )

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        // First test swiping from 0 to -1, which should no-op
        composeTestRule.onNodeWithText("0")
            .performGesture {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> swipeRight()
                    else -> swipeLeft()
                }
            }
        // ...and assert that nothing happened
        assertPagerLayout(
            currentPage = 0,
            pageCount = 10,
            offscreenLimit = offscreenLimit,
            expectedItemWidth = rootBounds.width * itemWidthFraction,
            layoutDirection = layoutDirection,
        )

        // Now swipe from page 0 to page 1
        composeTestRule.onNodeWithText("0")
            .performGesture {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> swipeLeft()
                    else -> swipeRight()
                }
            }
        // ...and assert that we now laid out from page 1
        assertPagerLayout(
            currentPage = 1,
            pageCount = 10,
            offscreenLimit = offscreenLimit,
            expectedItemWidth = rootBounds.width * itemWidthFraction,
            layoutDirection = layoutDirection,
        )
    }

    @Test
    fun mediumDistance_fastSwipe_toFling() {
        setPagerContent(
            layoutDirection = layoutDirection,
            pageModifier = Modifier.fillMaxWidth(itemWidthFraction),
            pageCount = 10,
            offscreenLimit = offscreenLimit,
        )

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        // Now swipe from page 0 to page 1, over a medium distance of the item width.
        // This should trigger a fling()
        composeTestRule.onNodeWithText("0")
            .swipeHorizontalAcrossCenter(
                distancePercentageX = when (layoutDirection) {
                    LayoutDirection.Rtl -> MediumSwipeDistance
                    else -> -MediumSwipeDistance
                },
                velocity = FastVelocity
            )
        // ...and assert that we now laid out from page 1
        assertPagerLayout(
            currentPage = 1,
            pageCount = 10,
            offscreenLimit = offscreenLimit,
            expectedItemWidth = rootBounds.width * itemWidthFraction,
            layoutDirection = layoutDirection,
        )
    }

    @Test
    fun mediumDistance_slowSwipe_toSnapForward() {
        setPagerContent(
            layoutDirection = layoutDirection,
            pageModifier = Modifier.fillMaxWidth(itemWidthFraction),
            pageCount = 10,
            offscreenLimit = offscreenLimit,
        )

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        // Now slowly swipe from page 0 to page 1, over a medium distance of the item width.
        // This should trigger a spring to position 1
        composeTestRule.onNodeWithText("0")
            .swipeHorizontalAcrossCenter(
                distancePercentageX = when (layoutDirection) {
                    LayoutDirection.Rtl -> MediumSwipeDistance
                    else -> -MediumSwipeDistance
                },
                velocity = SlowVelocity,
            )
        // ...and assert that we now laid out from page 1
        assertPagerLayout(
            currentPage = 1,
            pageCount = 10,
            offscreenLimit = offscreenLimit,
            expectedItemWidth = rootBounds.width * itemWidthFraction,
            layoutDirection = layoutDirection,
        )
    }

    @Test
    fun shortDistance_fastSwipe_toFling() {
        setPagerContent(
            layoutDirection = layoutDirection,
            pageModifier = Modifier.fillMaxWidth(itemWidthFraction),
            pageCount = 10,
            offscreenLimit = offscreenLimit,
        )

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        // Now swipe from page 0 to page 1, over a short distance of the item width.
        // This should trigger a fling to page 1
        composeTestRule.onNodeWithText("0")
            .swipeHorizontalAcrossCenter(
                distancePercentageX = when (layoutDirection) {
                    LayoutDirection.Rtl -> ShortSwipeDistance
                    else -> -ShortSwipeDistance
                },
                velocity = FastVelocity,
            )
        // ...and assert that we now laid out from page 1
        assertPagerLayout(
            currentPage = 1,
            pageCount = 10,
            offscreenLimit = offscreenLimit,
            expectedItemWidth = rootBounds.width * itemWidthFraction,
            layoutDirection = layoutDirection,
        )
    }

    @Test
    fun shortDistance_slowSwipe_toSnapBack() {
        setPagerContent(
            layoutDirection = layoutDirection,
            pageModifier = Modifier.fillMaxWidth(itemWidthFraction),
            pageCount = 10,
            offscreenLimit = offscreenLimit,
        )

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        // Now slowly swipe from page 0 to page 1, over a short distance of the item width.
        // This should trigger a spring back to the original position
        composeTestRule.onNodeWithText("0")
            .swipeHorizontalAcrossCenter(
                distancePercentageX = when (layoutDirection) {
                    LayoutDirection.Rtl -> ShortSwipeDistance
                    else -> -ShortSwipeDistance
                },
                velocity = SlowVelocity,
            )
        // ...and assert that we 'sprang back' to page 0
        assertPagerLayout(
            currentPage = 0,
            pageCount = 10,
            offscreenLimit = offscreenLimit,
            expectedItemWidth = rootBounds.width * itemWidthFraction,
            layoutDirection = layoutDirection,
        )
    }

    @Test
    @Ignore("Currently broken") // TODO: Will fix this once we move to Modifier.scrollable()
    fun a11yScroll() {
        setPagerContent(
            layoutDirection = layoutDirection,
            pageModifier = Modifier.fillMaxWidth(itemWidthFraction),
            pageCount = 10,
            offscreenLimit = offscreenLimit,
        )

        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        // Perform a scroll to item 1
        composeTestRule.onNodeWithText("1").performScrollTo()

        // ...and assert that we scrolled to page 1
        assertPagerLayout(
            currentPage = 1,
            pageCount = 10,
            offscreenLimit = offscreenLimit,
            expectedItemWidth = rootBounds.width * itemWidthFraction,
            layoutDirection = layoutDirection,
        )
    }

    // TODO: add test for state restoration?

    private fun assertPagerLayout(
        currentPage: Int,
        pageCount: Int,
        expectedItemWidth: Dp,
        offscreenLimit: Int,
        layoutDirection: LayoutDirection,
    ) {
        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        // The expected left of the first item. This uses the implicit fact that Pager
        // centers items horizontally.
        val firstItemLeft = (rootBounds.width - expectedItemWidth) / 2

        // The pages which are expected to be laid out, using the given current page,
        // offscreenLimit and page limit
        val expectedLaidOutPages = (currentPage - offscreenLimit)..(currentPage + offscreenLimit)
            .coerceIn(0, pageCount)

        // Go through all of the pages, and assert the expected layout state
        (0 until pageCount).forEach { page ->
            if (page in expectedLaidOutPages) {
                // If this page is expected to be laid out, assert that it exists and is
                // laid out in the correct position
                composeTestRule.onNodeWithText(page.toString())
                    .assertExists()
                    .assertWidthIsEqualTo(expectedItemWidth)
                    .assertWhen(layoutDirection == LayoutDirection.Ltr) {
                        assertLeftPositionInRootIsEqualTo(
                            firstItemLeft + (expectedItemWidth * (page - currentPage))
                        )
                    }
                    .assertWhen(layoutDirection == LayoutDirection.Rtl) {
                        assertLeftPositionInRootIsEqualTo(
                            firstItemLeft - (expectedItemWidth * (page - currentPage))
                        )
                    }
                    .assertIsSelectable()
                    .assertWhen(page == currentPage) { assertIsSelected() }
                    .assertWhen(page != currentPage) { assertIsNotSelected() }
            } else {
                // If this page is not expected to be laid out, assert that it doesn't exist
                composeTestRule.onNodeWithText(page.toString()).assertDoesNotExist()
            }
        }
    }

    private fun setPagerContent(
        layoutDirection: LayoutDirection,
        pageModifier: Modifier,
        pageCount: Int,
        offscreenLimit: Int,
    ): PagerState {
        val pagerState = PagerState(pageCount = pageCount)

        composeTestRule.setContent(layoutDirection) {
            HorizontalPager(
                state = pagerState,
                offscreenLimit = offscreenLimit,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                BasicText(page.toString(), pageModifier)
            }
        }
        return pagerState
    }
}
