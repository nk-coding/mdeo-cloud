package com.mdeo.modeltransformation.runtime

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertIs

class TransformationExecutionResultTest {

    @Nested
    inner class SuccessTests {

        @Test
        fun `Success with empty context and no modifications`() {
            val context = TransformationExecutionContext.empty()
            val result = TransformationExecutionResult.Success(context)
            
            assertTrue(result.isSuccess())
            assertFalse(result.isFailure())
            assertFalse(result.isStopped())
            assertTrue(result.matchedNodes.isEmpty())
            assertTrue(result.createdNodes.isEmpty())
            assertTrue(result.deletedNodes.isEmpty())
        }

        @Test
        fun `Success with matched nodes`() {
            val context = TransformationExecutionContext.empty()
            val result = TransformationExecutionResult.Success(
                context = context,
                matchedNodes = setOf("v1", "v2", "v3")
            )
            
            assertEquals(3, result.matchedNodes.size)
            assertTrue(result.matchedNodes.contains("v1"))
        }

        @Test
        fun `Success with created nodes`() {
            val context = TransformationExecutionContext.empty()
            val result = TransformationExecutionResult.Success(
                context = context,
                createdNodes = setOf("new1", "new2")
            )
            
            assertEquals(2, result.createdNodes.size)
            assertTrue(result.createdNodes.contains("new1"))
        }

        @Test
        fun `withMatchedNodes adds nodes`() {
            val context = TransformationExecutionContext.empty()
            val result = TransformationExecutionResult.Success(context)
                .withMatchedNodes("v1", "v2")
            
            assertEquals(2, result.matchedNodes.size)
        }

        @Test
        fun `withCreatedNodes adds nodes`() {
            val result = TransformationExecutionResult.Success(
                context = TransformationExecutionContext.empty()
            ).withCreatedNodes("new1")
            
            assertEquals(1, result.createdNodes.size)
            assertTrue(result.createdNodes.contains("new1"))
        }

        @Test
        fun `withDeletedNodes adds nodes`() {
            val result = TransformationExecutionResult.Success(
                context = TransformationExecutionContext.empty()
            ).withDeletedNodes("del1", "del2")
            
            assertEquals(2, result.deletedNodes.size)
        }

        @Test
        fun `withMatchedEdges adds edges`() {
            val result = TransformationExecutionResult.Success(
                context = TransformationExecutionContext.empty()
            ).withMatchedEdges("e1", "e2")
            
            assertEquals(2, result.matchedEdges.size)
        }

        @Test
        fun `withCreatedEdges adds edges`() {
            val result = TransformationExecutionResult.Success(
                context = TransformationExecutionContext.empty()
            ).withCreatedEdges("new-edge")
            
            assertEquals(1, result.createdEdges.size)
        }

        @Test
        fun `withDeletedEdges adds edges`() {
            val result = TransformationExecutionResult.Success(
                context = TransformationExecutionContext.empty()
            ).withDeletedEdges("del-edge")
            
            assertEquals(1, result.deletedEdges.size)
        }

        @Test
        fun `withContext updates context`() {
            val ctx1 = TransformationExecutionContext.empty()
            val ctx2 = ctx1.bindVariable("x", 42)
            
            val result = TransformationExecutionResult.Success(ctx1)
                .withContext(ctx2)
            
            assertEquals(42, result.context.lookupVariable("x"))
        }

        @Test
        fun `merge combines two Success results`() {
            val ctx = TransformationExecutionContext.empty().bindVariable("final", true)
            val result1 = TransformationExecutionResult.Success(
                context = TransformationExecutionContext.empty(),
                matchedNodes = setOf("v1"),
                createdNodes = setOf("new1")
            )
            val result2 = TransformationExecutionResult.Success(
                context = ctx,
                matchedNodes = setOf("v2"),
                deletedNodes = setOf("del1")
            )
            
            val merged = result1.merge(result2)
            
            assertEquals(true, merged.context.lookupVariable("final"))
            assertEquals(2, merged.matchedNodes.size)
            assertEquals(1, merged.createdNodes.size)
            assertEquals(1, merged.deletedNodes.size)
        }

        @Test
        fun `successOrNull returns Success`() {
            val result: TransformationExecutionResult = TransformationExecutionResult.Success(
                TransformationExecutionContext.empty()
            )
            
            val success = result.successOrNull()
            assertIs<TransformationExecutionResult.Success>(success)
        }
    }

    @Nested
    inner class FailureTests {

        @Test
        fun `Failure with reason`() {
            val result = TransformationExecutionResult.Failure(
                reason = "Pattern did not match"
            )
            
            assertFalse(result.isSuccess())
            assertTrue(result.isFailure())
            assertFalse(result.isStopped())
            assertEquals("Pattern did not match", result.reason)
            assertNull(result.context)
            assertNull(result.failedAt)
        }

        @Test
        fun `Failure with context`() {
            val context = TransformationExecutionContext.empty().bindVariable("x", 1)
            val result = TransformationExecutionResult.Failure(
                reason = "Error",
                context = context
            )
            
            assertEquals(1, result.context?.lookupVariable("x"))
        }

        @Test
        fun `Failure with failedAt`() {
            val result = TransformationExecutionResult.Failure(
                reason = "Error",
                failedAt = "match house: House"
            )
            
            assertEquals("match house: House", result.failedAt)
        }

        @Test
        fun `at method adds location`() {
            val result = TransformationExecutionResult.Failure("Error")
                .at("line 42")
            
            assertEquals("line 42", result.failedAt)
        }

        @Test
        fun `withContext method adds context`() {
            val ctx = TransformationExecutionContext.empty()
            val result = TransformationExecutionResult.Failure("Error")
                .withContext(ctx)
            
            assertEquals(ctx, result.context)
        }

        @Test
        fun `failureOrNull returns Failure`() {
            val result: TransformationExecutionResult = TransformationExecutionResult.Failure(
                "error"
            )
            
            val failure = result.failureOrNull()
            assertIs<TransformationExecutionResult.Failure>(failure)
        }

        @Test
        fun `failureOrNull returns null for Success`() {
            val result: TransformationExecutionResult = TransformationExecutionResult.Success(
                TransformationExecutionContext.empty()
            )
            
            assertNull(result.failureOrNull())
        }
    }

    @Nested
    inner class StoppedTests {

        @Test
        fun `Stopped with stop keyword`() {
            val context = TransformationExecutionContext.empty()
            val result = TransformationExecutionResult.Stopped(
                keyword = "stop",
                context = context
            )
            
            assertFalse(result.isSuccess())
            assertFalse(result.isFailure())
            assertTrue(result.isStopped())
            assertTrue(result.isNormalStop)
            assertFalse(result.isKill)
        }

        @Test
        fun `Stopped with kill keyword`() {
            val context = TransformationExecutionContext.empty()
            val result = TransformationExecutionResult.Stopped(
                keyword = "kill",
                context = context
            )
            
            assertTrue(result.isStopped())
            assertFalse(result.isNormalStop)
            assertTrue(result.isKill)
        }

        @Test
        fun `Stopped preserves context`() {
            val context = TransformationExecutionContext.empty()
                .bindVariable("x", 42)
            val result = TransformationExecutionResult.Stopped("stop", context)
            
            assertEquals(42, result.context.lookupVariable("x"))
        }
    }

    @Nested
    inner class ExtensionFunctionTests {

        @Test
        fun `isSuccess returns true only for Success`() {
            val success: TransformationExecutionResult = TransformationExecutionResult.Success(
                TransformationExecutionContext.empty()
            )
            val failure: TransformationExecutionResult = TransformationExecutionResult.Failure("err")
            val stopped: TransformationExecutionResult = TransformationExecutionResult.Stopped(
                "stop", TransformationExecutionContext.empty()
            )
            
            assertTrue(success.isSuccess())
            assertFalse(failure.isSuccess())
            assertFalse(stopped.isSuccess())
        }

        @Test
        fun `isFailure returns true only for Failure`() {
            val success: TransformationExecutionResult = TransformationExecutionResult.Success(
                TransformationExecutionContext.empty()
            )
            val failure: TransformationExecutionResult = TransformationExecutionResult.Failure("err")
            val stopped: TransformationExecutionResult = TransformationExecutionResult.Stopped(
                "stop", TransformationExecutionContext.empty()
            )
            
            assertFalse(success.isFailure())
            assertTrue(failure.isFailure())
            assertFalse(stopped.isFailure())
        }

        @Test
        fun `isStopped returns true only for Stopped`() {
            val success: TransformationExecutionResult = TransformationExecutionResult.Success(
                TransformationExecutionContext.empty()
            )
            val failure: TransformationExecutionResult = TransformationExecutionResult.Failure("err")
            val stopped: TransformationExecutionResult = TransformationExecutionResult.Stopped(
                "stop", TransformationExecutionContext.empty()
            )
            
            assertFalse(success.isStopped())
            assertFalse(failure.isStopped())
            assertTrue(stopped.isStopped())
        }

        @Test
        fun `successOrNull returns null for non-Success`() {
            val failure: TransformationExecutionResult = TransformationExecutionResult.Failure("err")
            
            assertNull(failure.successOrNull())
        }
    }
}
