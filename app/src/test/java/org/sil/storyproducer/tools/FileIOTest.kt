package org.sil.storyproducer.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class FileIOTest {
    @Test
    fun testDeleteStoryFile() {
        // we need a context, a relative path, and a directory root
        // ideally we create the file in some mock environment
        // then we delete it

        // also, why do we have this function and also a `deleteWorkspaceFile` function?
    }

}
