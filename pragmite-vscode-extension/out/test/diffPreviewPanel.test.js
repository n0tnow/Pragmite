"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
const assert = __importStar(require("assert"));
const vscode = __importStar(require("vscode"));
const diffPreviewPanel_1 = require("../diffPreviewPanel");
/**
 * Test suite for DiffPreviewPanel with Monaco Editor integration
 * v1.6.3 - Integration Sprint Monaco Editor Task
 */
suite('DiffPreviewPanel Test Suite', () => {
    const extensionPath = vscode.extensions.getExtension('pragmite.pragmite')?.extensionPath || '';
    test('Should create DiffPreviewPanel with Monaco Editor', () => {
        const beforeCode = `public class Example {
    private int value;

    public void oldMethod() {
        System.out.println("Old");
    }
}`;
        const afterCode = `public class Example {
    private int value;

    public void newMethod() {
        System.out.println("New");
    }

    public int getValue() {
        return value;
    }
}`;
        // Create the panel
        diffPreviewPanel_1.DiffPreviewPanel.createOrShow(extensionPath, 'Example.java', beforeCode, afterCode, 'Extract Method');
        // Verify panel was created
        assert.ok(diffPreviewPanel_1.DiffPreviewPanel.currentPanel, 'Panel should be created');
    });
    test('Should reuse existing panel when called twice', () => {
        const beforeCode = 'public class Test {}';
        const afterCode = 'public class Test { int x; }';
        // Create first panel
        diffPreviewPanel_1.DiffPreviewPanel.createOrShow(extensionPath, 'Test.java', beforeCode, afterCode, 'Add Field');
        const firstPanel = diffPreviewPanel_1.DiffPreviewPanel.currentPanel;
        // Create second panel
        diffPreviewPanel_1.DiffPreviewPanel.createOrShow(extensionPath, 'Test.java', beforeCode, afterCode, 'Add Field');
        const secondPanel = diffPreviewPanel_1.DiffPreviewPanel.currentPanel;
        // Should reuse the same panel
        assert.strictEqual(firstPanel, secondPanel, 'Should reuse existing panel');
    });
    test('Should handle complex Java refactoring diff', () => {
        const beforeCode = `package com.example;

import java.util.List;

public class UserService {
    private List<User> users;

    public void processUsers() {
        for (User user : users) {
            if (user.isActive()) {
                System.out.println(user.getName());
            }
        }
    }
}`;
        const afterCode = `package com.example;

import java.util.List;
import java.util.stream.Collectors;

public class UserService {
    private List<User> users;

    public void processUsers() {
        users.stream()
            .filter(User::isActive)
            .forEach(user -> System.out.println(user.getName()));
    }

    public List<User> getActiveUsers() {
        return users.stream()
            .filter(User::isActive)
            .collect(Collectors.toList());
    }
}`;
        // Create panel with complex diff
        diffPreviewPanel_1.DiffPreviewPanel.createOrShow(extensionPath, 'UserService.java', beforeCode, afterCode, 'Convert to Stream API');
        assert.ok(diffPreviewPanel_1.DiffPreviewPanel.currentPanel, 'Panel should handle complex diff');
    });
    test('Should dispose panel correctly', () => {
        const beforeCode = 'class A {}';
        const afterCode = 'class A { int x; }';
        diffPreviewPanel_1.DiffPreviewPanel.createOrShow(extensionPath, 'A.java', beforeCode, afterCode, 'Test');
        assert.ok(diffPreviewPanel_1.DiffPreviewPanel.currentPanel, 'Panel should exist before dispose');
        // Dispose the panel
        diffPreviewPanel_1.DiffPreviewPanel.currentPanel?.dispose();
        assert.strictEqual(diffPreviewPanel_1.DiffPreviewPanel.currentPanel, undefined, 'Panel should be undefined after dispose');
    });
    test('Should handle empty code gracefully', () => {
        const beforeCode = '';
        const afterCode = 'public class New {}';
        // Should not throw
        assert.doesNotThrow(() => {
            diffPreviewPanel_1.DiffPreviewPanel.createOrShow(extensionPath, 'New.java', beforeCode, afterCode, 'Create Class');
        }, 'Should handle empty before code');
        assert.ok(diffPreviewPanel_1.DiffPreviewPanel.currentPanel, 'Panel should be created even with empty before code');
        diffPreviewPanel_1.DiffPreviewPanel.currentPanel?.dispose();
    });
    test('Should handle code with special characters', () => {
        const beforeCode = 'String s = "hello";';
        const afterCode = 'String s = "hello\\nworld\\t";';
        // Should escape special characters correctly
        assert.doesNotThrow(() => {
            diffPreviewPanel_1.DiffPreviewPanel.createOrShow(extensionPath, 'Test.java', beforeCode, afterCode, 'Update String');
        }, 'Should handle special characters');
        diffPreviewPanel_1.DiffPreviewPanel.currentPanel?.dispose();
    });
    test('Should handle code with backticks and template literals', () => {
        const beforeCode = 'String template = "simple";';
        const afterCode = 'String template = "`complex`";';
        // Should escape backticks for JavaScript embedding
        assert.doesNotThrow(() => {
            diffPreviewPanel_1.DiffPreviewPanel.createOrShow(extensionPath, 'Template.java', beforeCode, afterCode, 'Update Template');
        }, 'Should handle backticks');
        diffPreviewPanel_1.DiffPreviewPanel.currentPanel?.dispose();
    });
});
//# sourceMappingURL=diffPreviewPanel.test.js.map